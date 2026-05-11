package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;

public interface SidingExtraSupplier {
    boolean isSpeedLimitEnabled();
    void setSpeedLimitEnabled(boolean value);

    int getSpeedLimit();
    void setSpeedLimit(int value);

    void updateSpeedLimit(boolean enabled, int speedLimit, Consumer<FriendlyByteBuf> sendPacket);
}
