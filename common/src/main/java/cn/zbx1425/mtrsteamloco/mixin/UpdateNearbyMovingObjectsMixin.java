package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.NameColorDataBase;
import mtr.data.UpdateNearbyMovingObjects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(UpdateNearbyMovingObjects.class)
public abstract class UpdateNearbyMovingObjectsMixin<T extends NameColorDataBase> {
    @Mutable
    @Shadow(remap = false)
    @Final
    public Map<Player, Set<T>> newDataSetInPlayerRange;

    @Mutable
    @Shadow(remap = false)
    @Final
    public Set<T> dataSetToSync;

    @Mutable
    @Shadow(remap = false)
    @Final
    private Map<Player, Set<T>> dataSetInPlayerRange;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(ResourceLocation deletePacketId, ResourceLocation updatePacketId, CallbackInfo ci) {
        this.newDataSetInPlayerRange = new ConcurrentHashMap<>(this.newDataSetInPlayerRange);
        this.dataSetToSync = Collections.synchronizedSet(this.dataSetToSync);
        this.dataSetInPlayerRange = new ConcurrentHashMap<>(this.dataSetInPlayerRange);
    }
}
