package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.util.ModExecutors;
import net.minecraft.world.phys.Vec3;
import mtr.data.*;
import cn.zbx1425.mtrsteamloco.data.*;
import io.netty.buffer.Unpooled;
import mtr.MTR;
import mtr.Registry;
import mtr.packet.*;
import mtr.path.PathData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.concurrent.*;
import java.util.stream.Collectors;

import java.util.*;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RailwayData.class)
public class RailwayDataMixin implements IPacket {

    @Shadow(remap = false) @Mutable @Final private Set<Station> stations = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private Set<Platform> platforms = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private Set<Siding> sidings = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private Set<Route> routes = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private Set<Depot> depots = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private Set<LiftServer> lifts = new HashSet<>();
	@Shadow(remap = false) @Mutable @Final private DataCache dataCache = new DataCache(stations, platforms, sidings, routes, depots, lifts);

	@Shadow(remap = false) private RailwayDataLoggingModule railwayDataLoggingModule;
	@Shadow(remap = false) private RailwayDataCoolDownModule railwayDataCoolDownModule;
	@Shadow(remap = false) private RailwayDataPathGenerationModule railwayDataPathGenerationModule;
	@Shadow(remap = false) private RailwayDataRailActionsModule railwayDataRailActionsModule;
	@Shadow(remap = false) private RailwayDataDriveTrainModule railwayDataDriveTrainModule;
	@Shadow(remap = false) private RailwayDataRouteFinderModule railwayDataRouteFinderModule;

	@Shadow(remap = false) private int prevPlatformCount;
	@Shadow(remap = false) private int prevSidingCount;
	@Shadow(remap = false) private boolean useTimeAndWindSync;

	@Shadow(remap = false) private Level world;
	@Shadow(remap = false) private Map<BlockPos, Map<BlockPos, Rail>> rails = new HashMap<>();
	@Shadow(remap = false) private SignalBlocks signalBlocks = new SignalBlocks();

	@Shadow(remap = false) private RailwayDataFileSaveModule railwayDataFileSaveModule;

	@Shadow(remap = false) @Mutable @Final private List<Map<UUID, Long>> trainPositions = new ArrayList<>(2);
	@Shadow(remap = false) @Mutable @Final private Map<Player, BlockPos> playerLastUpdatedPositions = new HashMap<>();
	@Shadow(remap = false) @Mutable @Final private List<Player> playersToSyncSchedules = new ArrayList<>();
	@Shadow(remap = false) private UpdateNearbyMovingObjects<TrainServer> updateNearbyTrains;
	@Shadow(remap = false) private UpdateNearbyMovingObjects<LiftServer> updateNearbyLifts;
	@Shadow(remap = false) @Mutable @Final private Map<Long, List<ScheduleEntry>> schedulesForPlatform = new HashMap<>();
	@Shadow(remap = false) @Mutable @Final private Map<Long, Map<BlockPos, TrainDelay>> trainDelays = new HashMap<>();

    @Shadow(remap = false) @Final @Mutable private static int RAIL_UPDATE_DISTANCE = 128;
	@Shadow(remap = false) private static int PLAYER_MOVE_UPDATE_THRESHOLD = 16;
	@Shadow(remap = false) private static int SCHEDULE_UPDATE_TICKS = 60;

	@Shadow(remap = false) private static int DATA_VERSION = 1;

	@Shadow(remap = false) private static String NAME = "mtr_train_data";
	@Shadow(remap = false) private static String KEY_RAW_MESSAGE_PACK = "raw_message_pack";
	@Shadow(remap = false) private static String KEY_DATA_VERSION = "mtr_data_version";
	@Shadow(remap = false) private static String KEY_STATIONS = "stations";
	@Shadow(remap = false) private static String KEY_PLATFORMS = "platforms";
	@Shadow(remap = false) private static String KEY_SIDINGS = "sidings";
	@Shadow(remap = false) private static String KEY_ROUTES = "routes";
	@Shadow(remap = false) private static String KEY_DEPOTS = "depots";
	@Shadow(remap = false) private static String KEY_LIFTS = "lifts";
	@Shadow(remap = false) private static String KEY_RAILS = "rails";
	@Shadow(remap = false) private static String KEY_SIGNAL_BLOCKS = "signal_blocks";
	@Shadow(remap = false) private static String KEY_USE_TIME_AND_WIND_SYNC = "use_time_and_wind_sync";

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(Level world, CallbackInfo ci) {
		this.stations = Collections.synchronizedSet(this.stations);
		this.platforms = Collections.synchronizedSet(this.platforms);
		this.sidings = Collections.synchronizedSet(this.sidings);
		this.routes = Collections.synchronizedSet(this.routes);
		this.depots = Collections.synchronizedSet(this.depots);
		this.lifts = Collections.synchronizedSet(this.lifts);

		this.dataCache = new DataCache(this.stations, this.platforms, this.sidings, this.routes, this.depots, this.lifts);

		this.schedulesForPlatform = new ConcurrentHashMap<>(this.schedulesForPlatform);
		this.playerLastUpdatedPositions = new ConcurrentHashMap<>(this.playerLastUpdatedPositions);
		this.playersToSyncSchedules = Collections.synchronizedList(this.playersToSyncSchedules);
		this.trainDelays = new ConcurrentHashMap<>(this.trainDelays);
		this.trainPositions = Collections.synchronizedList(this.trainPositions);
	}

	@Unique
	private void mtrSteamLoco$sendPlayersUpdates() {
		RAIL_UPDATE_DISTANCE = world.getServer().getPlayerList().getViewDistance() * 16;

		var tasks = new ArrayList<CompletableFuture<?>>();

		for (var player : world.players()) {
			tasks.add(
					CompletableFuture.runAsync(() -> this.mtrSteamLoco$sendPlayerUpdates(player), ModExecutors.SIMULATION)
			);
		}

		try {
			CompletableFuture.allOf(
					tasks.toArray(new CompletableFuture<?>[0])
			).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Unique
	private void mtrSteamLoco$sendPlayerUpdates(Player player) {
//		var playerBlockPos = player.blockPosition();
//
//		var lastPos = playerLastUpdatedPositions.get(player);
//
//		if (lastPos != null && lastPos.distManhattan(playerBlockPos) <= PLAYER_MOVE_UPDATE_THRESHOLD) {
//			return;
//		}
//
//		playerLastUpdatedPositions.put(player, playerBlockPos);
//
//		var size = 0;
//		var packet = new FriendlyByteBuf(Unpooled.buffer());
//
//		var sizeIdx = packet.writerIndex();
//		packet.writeInt(0);
//
//		for (var endPositions : rails.entrySet()) {
//			var startPos = endPositions.getKey();
//
//			packet.writeBlockPos(startPos);
//
//			var railCountIdx = packet.writerIndex();
//			packet.writeInt(0);
//
//			var railCount = 0;
//
//			for (var entry : endPositions.getValue().entrySet()) {
//				var endPos = entry.getKey();
//				var rail = entry.getValue();
//
//				if (!((RailExtraSupplier) rail).isBetween(player.getX(), player.getY(), player.getZ(), RAIL_UPDATE_DISTANCE)) {
//					continue;
//				}
//
//				packet.writeBlockPos(endPos);
//				rail.writePacket(packet);
//
//				railCount++;
//			}
//
//			packet.setInt(railCountIdx, railCount);
//
//			size++;
//		}
//
//		if (packet.readableBytes() > MAX_PACKET_BYTES) {
//			return;
//		}
//
//		packet.setInt(sizeIdx, size);
//
//		Registry.sendToPlayer((ServerPlayer) player, PACKET_WRITE_RAILS, packet);

		BlockPos playerBlockPos = player.blockPosition();
		Vec3 playerPos = player.position();

		if (!playerLastUpdatedPositions.containsKey(player) || playerLastUpdatedPositions.get(player).distManhattan(playerBlockPos) > PLAYER_MOVE_UPDATE_THRESHOLD) {
			Map<BlockPos, Map<BlockPos, Rail>> railsToAdd = new HashMap<>();
			rails.forEach((startPos, blockPosRailMap) -> blockPosRailMap.forEach((endPos, rail) -> {
				if (((RailExtraSupplier) (Object) rail).isBetween(playerPos.x, playerPos.y, playerPos.z, RAIL_UPDATE_DISTANCE)) {
					if (!railsToAdd.containsKey(startPos)) {
						railsToAdd.put(startPos, new HashMap<>());
					}
					railsToAdd.get(startPos).put(endPos, rail);
				}
			}));

			FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
			packet.writeInt(railsToAdd.size());
			railsToAdd.forEach((posStart, railMap) -> {
				packet.writeBlockPos(posStart);
				packet.writeInt(railMap.size());
				railMap.forEach((posEnd, rail) -> {
					packet.writeBlockPos(posEnd);
					rail.writePacket(packet);
				});
			});

			if (packet.readableBytes() <= MAX_PACKET_BYTES) {
				Registry.sendToPlayer((ServerPlayer) player, PACKET_WRITE_RAILS, packet);
			}
			playerLastUpdatedPositions.put(player, playerBlockPos);
		}
	}

	@Unique
	private void mtrSteamLoco$tickSidings() {
		schedulesForPlatform.clear();

		trainPositions.remove(0);
		trainPositions.add(new HashMap<>());

		var tasks = new CompletableFuture[sidings.size()];
		var iterator = sidings.iterator();

		for (var i = 0; i < tasks.length; i++) {
			var siding = iterator.next();

			tasks[i] = CompletableFuture.runAsync(() -> {
				siding.setSidingData(world, dataCache.sidingIdToDepot.get(siding.id), rails);
				siding.simulateTrain(dataCache, railwayDataDriveTrainModule, trainPositions, signalBlocks, updateNearbyTrains.newDataSetInPlayerRange, updateNearbyTrains.dataSetToSync, schedulesForPlatform, trainDelays);
			}, ModExecutors.SIMULATION);
		}

        try {
            CompletableFuture.allOf(tasks).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

	@Unique
	private void mtrSteamLoco$updateSchedule() {
		if (MTR.isGameTickInterval(SCHEDULE_UPDATE_TICKS)) {
			playersToSyncSchedules.clear();
            playersToSyncSchedules.addAll(world.players());
		}

		if (!playersToSyncSchedules.isEmpty()) {
			Player player = playersToSyncSchedules.remove(0);
			BlockPos playerBlockPos = player.blockPosition();
			Vec3 playerPos = player.position();

			Set<Long> platformIds = platforms.stream().filter(platform -> {
				if (platform.isCloseToSavedRail(playerBlockPos, PLAYER_MOVE_UPDATE_THRESHOLD, PLAYER_MOVE_UPDATE_THRESHOLD, PLAYER_MOVE_UPDATE_THRESHOLD)) {
					return true;
				}
				Station station = dataCache.platformIdToStation.get(platform.id);
				return station != null && station.inArea(playerBlockPos.getX(), playerBlockPos.getZ());
			}).map(platform -> platform.id).collect(Collectors.toSet());

			Set<UUID> railsToAdd = new HashSet<>();
			rails.forEach((startPos, blockPosRailMap) -> blockPosRailMap.forEach((endPos, rail) -> {
				if (((RailExtraSupplier) (Object) rail).isBetween(playerPos.x, playerPos.y, playerPos.z, RAIL_UPDATE_DISTANCE)) {
					railsToAdd.add(PathData.getRailProduct(startPos, endPos));
				}
			}));

			Map<Long, Boolean> signalBlockStatus = new HashMap<>();
			Map<UUID, Boolean> occupiedRails = new HashMap<>();
			railsToAdd.forEach(rail -> {
				signalBlocks.getSignalBlockStatus(signalBlockStatus, rail);
				occupiedRails.put(rail, trainPositions.get(1).containsKey(rail));
			});

			if (!platformIds.isEmpty() || !signalBlockStatus.isEmpty() || !occupiedRails.isEmpty()) {
				FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
				packet.writeInt(platformIds.size());
				platformIds.forEach(platformId -> {
					packet.writeLong(platformId);
					List<ScheduleEntry> scheduleEntries = schedulesForPlatform.get(platformId);
					if (scheduleEntries == null) {
						packet.writeInt(0);
					} else {
						scheduleEntries = new ArrayList<>(scheduleEntries);

						packet.writeInt(scheduleEntries.size());
						scheduleEntries.forEach(scheduleEntry -> scheduleEntry.writePacket(packet));
					}
				});

				packet.writeInt(signalBlockStatus.size());
				signalBlockStatus.forEach((id, occupied) -> {
					packet.writeLong(id);
					packet.writeBoolean(occupied);
				});

				packet.writeInt(occupiedRails.size());
				occupiedRails.forEach((rail, occupied) -> {
					packet.writeUUID(rail);
					packet.writeBoolean(occupied);
				});

				if (packet.readableBytes() <= MAX_PACKET_BYTES) {
					Registry.sendToPlayer((ServerPlayer) player, PACKET_UPDATE_SCHEDULE, packet);
				}
			}
		}
	}

	private void mtrSteamLoco$syncDataCache() {
		if (prevPlatformCount != platforms.size() || prevSidingCount != sidings.size()) {
			dataCache.sync();
		}

		prevPlatformCount = platforms.size();
		prevSidingCount = sidings.size();
	}

    public void simulateTrains() {
		var tasks = new ArrayList<CompletableFuture<?>>();

		tasks.add(
				CompletableFuture.runAsync(this::mtrSteamLoco$sendPlayersUpdates, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(updateNearbyLifts::startTick, ModExecutors.SIMULATION)
						.thenRunAsync(() -> {
							var liftTasks = new CompletableFuture[lifts.size()];
							var iterator = lifts.iterator();

							for (var i = 0; i < liftTasks.length; i++) {
								var lift = iterator.next();

								liftTasks[i] = CompletableFuture.runAsync(() -> {
									lift.tickServer(world, updateNearbyLifts.newDataSetInPlayerRange, updateNearbyLifts.dataSetToSync);
								}, ModExecutors.SIMULATION);
							}

							try {
								CompletableFuture.allOf(liftTasks).get();
							} catch (InterruptedException | ExecutionException e) {
								throw new RuntimeException(e);
							}
						}, ModExecutors.SIMULATION)
						.thenRunAsync(updateNearbyLifts::tick, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(signalBlocks::resetOccupied, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture
						.runAsync(updateNearbyTrains::startTick, ModExecutors.SIMULATION)
						.thenRunAsync(this::mtrSteamLoco$tickSidings, ModExecutors.SIMULATION)
						.thenRunAsync(() -> {
							var depotTasks = new CompletableFuture[depots.size()];
							var iterator = depots.iterator();

							for (var i = 0; i < depotTasks.length; i++) {
								var depot = iterator.next();

								depotTasks[i] = CompletableFuture.runAsync(() -> {
									depot.deployTrain((RailwayData) (Object) this, world);
								}, ModExecutors.SIMULATION);
							}

                            try {
                                CompletableFuture.allOf(depotTasks).get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
						}, ModExecutors.SIMULATION)
						.thenRunAsync(updateNearbyTrains::tick, ModExecutors.SIMULATION)
						.thenRunAsync(this::mtrSteamLoco$updateSchedule, ModExecutors.SIMULATION)
						.thenRunAsync(railwayDataRouteFinderModule::tick, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(railwayDataCoolDownModule::tick, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(railwayDataDriveTrainModule::tick, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(railwayDataFileSaveModule::autoSaveTick, ModExecutors.SIMULATION)
		);

		tasks.add(
				CompletableFuture.runAsync(this::mtrSteamLoco$syncDataCache, ModExecutors.SIMULATION)
		);

        try {
            CompletableFuture.allOf(
                    tasks.toArray(new CompletableFuture<?>[0])
            ).get(120L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Error during async simulation.", e);
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out waiting for async simulation to finish. " + tasks, e);
		}

		railwayDataRailActionsModule.tick();
    }
}