package cn.zbx1425.sowcer.math;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;

public class Vector3d {
    protected Vec3 impl;

    public Vector3d(double x, double y, double z) {
        this.impl = new Vec3(x, y, z);
    }

    public double x() { return impl.x(); }
    public double y() { return impl.y(); }
    public double z() { return impl.z(); }

    private Vector3d(Vector3d other) {
        this.impl = other.impl;
    }

    public Vector3d(Vector3f other) {
        this(other.x(), other.y(), other.z());
    }

    public Vector3d(Vec3 moj) {
        this.impl = moj;
    }

    public Vector3d copy() {
        return new Vector3d(this);
    }

    public void normalize() {
        this.impl = this.impl.normalize();
    }

    public void add(double x, double y, double z) {
        this.impl = this.impl.add(x, y, z);
    }

    public void add(Vector3d other) {
        this.impl = this.impl.add(other.impl);
    }

    public void add(Vector3f other) {
        this.add(new Vector3d(other));
    }

    public void sub(Vector3d other) {
        this.impl = this.impl.subtract(other.impl);
    }

    public void sub(Vector3f other) {
        this.sub(new Vector3d(other));
    }

    public void mul(double x, double y, double z) {
        this.impl = this.impl.multiply(x, y, z);
    }

    public void mul(double n) {
        this.impl = this.impl.scale(n);
    }

    public void rot(Vector3d axis, float rad) {
        var mat = new Matrix4d().rotate(rad, axis.x(), axis.y(), axis.z());

        var transformed = mat.transformPosition(this.impl.x, this.impl.y, this.impl.z, new org.joml.Vector3d());

        this.impl = new Vec3(
                transformed.x(),
                transformed.y(),
                transformed.z()
        );
    }

    public void rot(Vector3f axis, float rad) {
        this.rot(new Vector3d(axis), rad);
    }

    public void rotDeg(Vector3d axis, float deg) {
        this.rot(axis, (float) Math.toRadians(deg));
    }

    public void rotDDef(Vector3f axis, float def) {
        this.rot(new Vector3d(axis), def);
    }

    public void rotX(float rad) {
        this.impl = this.impl.xRot(rad);
    }

    public void rotY(float rad) {
        this.impl = this.impl.yRot(rad);
    }

    public void rotZ(float rad) {
        this.impl = this.impl.zRot(rad);
    }

    public void cross(Vector3d other) {
        this.impl = this.impl.cross(other.impl);
    }

    public void cross(Vector3f other) {
        this.cross(new Vector3d(other));
    }

    public Vec3 asMoj() {
        return impl;
    }

    @Override
    public int hashCode() {
        return this.impl.hashCode();
    }

    @Override
    public String toString() {
        return "(" + this.x() + ", " + this.y() + ", " + this.z() + ")";
    }

    public double distance(Vector3d other) {
        return this.impl.distanceTo(other.impl);
    }

    public double distance(Vector3f other) {
        return this.distance(new Vector3d(other));
    }

    public double distanceSq(Vector3d other) {
        return this.impl.distanceToSqr(other.impl);
    }

    public double distanceSq(Vector3f other) {
        return this.distanceSq(new Vector3d(other));
    }

    public Vector3d(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos((int) this.x(), (int) this.y(), (int) this.z());
    }

    public Vec3 toVec3() {
        return this.impl;
    }

    public double lengthSquared() {
        return this.impl.lengthSqr();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        var other = (Vector3d) obj;

        return this.impl.equals(other.impl);
    }

    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d XP = new Vector3d(1, 0, 0);
    public static final Vector3d YP = new Vector3d(0, 1, 0);
    public static final Vector3d ZP = new Vector3d(0, 0, 1);
}
