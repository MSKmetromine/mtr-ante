package cn.zbx1425.mtrsteamloco.data;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;

public interface SidingExtraSupplier {
    boolean isSpeedLimitEnabled();
    void setSpeedLimitEnabled(boolean value);

    int getSpeedLimit();
    void setSpeedLimit(int value);

    boolean isDecelerationConstantEnabled();
    void setDecelerationConstantEnabled(boolean value);

    float getDecelerationConstant();
    void setDecelerationConstant(float value);

    void updateSpeedLimit(boolean enabled, int speedLimit, Consumer<FriendlyByteBuf> sendPacket);
    void updateDecelerationConstant(boolean enabled, float decelerationConstant, Consumer<FriendlyByteBuf> sendPacket);
}
