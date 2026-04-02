package cn.zbx1425.mtrsteamloco.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record CarPosition(PositionRotation car, List<PositionRotation> bogies) {
    public List<PositionRotation> bogiesRelative() {
        List<PositionRotation> newBogies = new ArrayList<>();

        for (PositionRotation bogie : this.bogies) {
            Vec3 position = bogie.position().subtract(car.position())
                    .yRot(-car.yaw())
                    .xRot(-car.pitch());

            float yaw = bogie.yaw() - car.yaw();
            float pitch = bogie.pitch() - car.pitch();

            newBogies.add(new PositionRotation(position, yaw, pitch));
        }

        return newBogies;
    }
}
