package cn.zbx1425.mtrsteamloco.util;

import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.world.phys.Vec3;

public record PositionRotation(Vec3 position, float yaw, float pitch) {
    public ScriptPositionRotation toScript() {
        return new ScriptPositionRotation(new Vector3f(this.position()), this.yaw(), this.pitch());
    }
}
