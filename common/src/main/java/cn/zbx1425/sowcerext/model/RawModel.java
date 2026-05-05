package cn.zbx1425.sowcerext.model;

import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.util.AttrUtil;
import cn.zbx1425.sowcer.util.DrawContext;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcer.vertex.VertAttrState;
import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import cn.zbx1425.sowcer.model.Mesh;
import java.util.function.Function;

public class RawModel {

    public ResourceLocation sourceLocation;

    @Deprecated
    public final HashMap<MaterialProp, RawMesh> meshList = new HashMap<>();

    public Future<?> loadTask;
    public final List<Runnable> postLoadTasks = new ArrayList<>();

    public RawModel() {

    }

    public RawModel(DataInputStream dis) throws IOException {
        int count = dis.readInt();
        for (int i = 0; i < count; i++) {
            RawMesh mesh = new RawMesh(dis);
            this.append(mesh);
        }
    }

    public boolean isLoaded() {
        return this.loadTask == null || this.loadTask.isDone();
    }

    public void waitForLoad() {
        if (this.loadTask == null) {
            return;
        }

        try {
            this.loadTask.get(60L, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void submitPostLoadTask(Runnable runnable) {
        if (this.loadTask == null || this.loadTask.isDone()) {
            runnable.run();
            return;
        }

        this.postLoadTasks.add(runnable);
    }

    public Model upload(VertAttrMapping mapping) {
        this.waitForLoad();

        Model model = new Model();
        for (RawMesh mesh : meshList.values()) {
            if (mesh.faces.isEmpty()) continue;
            model.meshList.add(mesh.upload(mapping));
        }
        return model;
    }

    public Supplier<Model> uploadAsync(VertAttrMapping mapping) {
        this.waitForLoad();

        Model model = new Model();
        Set<Runnable> uploadTasks = new HashSet<>();
        for (RawMesh mesh : meshList.values()) {
            if (mesh.faces.isEmpty()) continue;
            Supplier<Mesh> meshSupplier = mesh.uploadAsync(mapping);
            uploadTasks.add(() -> {
                model.meshList.add(meshSupplier.get());
            });
        }
        return () -> {
            for (Runnable task : uploadTasks) task.run();
            return model;
        };
    }

    public void append(RawMesh nextMesh) {
        if (meshList.containsKey(nextMesh.materialProp)) {
            RawMesh mesh = meshList.get(nextMesh.materialProp);
            mesh.append(nextMesh);
        } else {
            RawMesh newMesh = new RawMesh(nextMesh.materialProp);
            meshList.put(nextMesh.materialProp, newMesh);
            newMesh.append(nextMesh);
        }
    }

    public void appendTransformed(RawModel nextModel, Matrix4f mat, int color, int light) {
        nextModel.waitForLoad();

        for (RawMesh nextMesh : nextModel.meshList.values()) {
            if (meshList.containsKey(nextMesh.materialProp)) {
                RawMesh mesh = meshList.get(nextMesh.materialProp);
                mesh.appendTransformed(nextMesh, mat, color, light);
            } else {
                RawMesh newMesh = new RawMesh(nextMesh.materialProp);
                meshList.put(nextMesh.materialProp, newMesh);
                newMesh.appendTransformed(nextMesh, mat, color, light);
            }
        }
    }

    public int getVertexCount() {
        if (!this.isLoaded()) {
            return 0;
        }

        int result = 0;
        for (RawMesh mesh : meshList.values()) {
            result += mesh.vertices.size();
        }
        return result;
    }

    public int getFaceCount() {
        if (!this.isLoaded()) {
            return 0;
        }

        int result = 0;
        for (RawMesh mesh : meshList.values()) {
            result += mesh.faces.size();
        }
        return result;
    }

    public void append(Collection<RawMesh> nextMesh) {
        for (RawMesh mesh : nextMesh) append(mesh);
    }

    public void append(RawModel nextModel) {
        nextModel.waitForLoad();
        append(nextModel.meshList.values());
    }

    public void applyMatrix(Matrix4f matrix) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyMatrix(matrix);
        });
    }

    public void applyTranslation(float x, float y, float z) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyTranslation(x, y, z);
        });
    }

    public void applyRotation(Vector3f axis, float angle) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyRotation(axis, angle);
        });
    }

    public void applyScale(float x, float y, float z) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyScale(x, y, z);
        });
    }

    public void applyMirror(boolean vx, boolean vy, boolean vz, boolean nx, boolean ny, boolean nz) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyMirror(vx, vy, vz, nx, ny, nz);
        });
    }

    public void applyUVMirror(boolean u, boolean v) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyUVMirror(u, v);
        });
    }

    public void generateNormals() {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.generateNormals();
        });
    }

    public void distinct() {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.distinct();
        });
    }

    public void triangulate() {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.triangulate();
        });
    }

    public void applyShear(Vector3f dir, Vector3f shear, float ratio) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) mesh.applyShear(dir, shear, ratio);
        });
    }

    private Map<RawMesh, MaterialProp> originalMaterialProps;

    public void setAllRenderType(String renderType) {
        this.submitPostLoadTask(() -> {
            if (originalMaterialProps == null) {
                originalMaterialProps = new HashMap<>();
                for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
                    originalMaterialProps.put(entry.getValue(), entry.getKey().copy());
                }
            }
            for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
                if (renderType.equals("reset")) {
                    MaterialProp originalProp = originalMaterialProps.get(entry.getValue());
                    if (originalProp != null) {
                        entry.getValue().materialProp.copyFrom(originalProp);
                        entry.getKey().copyFrom(originalProp);
                    } else {
                        entry.getValue().setRenderType(renderType);
                    }
                } else {
                    entry.getValue().setRenderType(renderType);
                }
                entry.getKey().shaderName = entry.getValue().materialProp.shaderName;
            }
        });
    }

    public void replaceTexture(String oldTexture, ResourceLocation newTexture) {
        this.submitPostLoadTask(() -> {
            for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
                if (entry.getKey().texture == null) continue;
                String oldPath = entry.getKey().texture.getPath();
                if (oldPath.substring(oldPath.lastIndexOf("/") + 1).equals(oldTexture)) {
                    entry.getValue().materialProp.texture = newTexture;
                    entry.getKey().texture = newTexture;
                }
            }
        });
    }

    public void replaceAllTexture(ResourceLocation newTexture) {
        this.submitPostLoadTask(() -> {
            for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
                entry.getValue().materialProp.texture = newTexture;
                entry.getKey().texture = newTexture;
            }
        });
    }

    public void clearAttrState(VertAttrType attrType) {
        this.submitPostLoadTask(() -> {
            for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
                entry.getKey().attrState.clearAttr(attrType);
            }
        });
    }

    public void writeBlazeBuffer(BufferSourceProxy vertexConsumers, Matrix4f matrix, int light, int overlay, DrawContext drawContext) {
        if (!this.isLoaded()) {
            return;
        }

        if (meshList.isEmpty()) return;
        for (Map.Entry<MaterialProp, RawMesh> entry : meshList.entrySet()) {
            RenderType renderType = entry.getKey().getBlazeRenderType();
            int resultColor = entry.getKey().attrState.color != null ? entry.getKey().attrState.color : 0xFFFFFFFF;
            int resultLight = entry.getKey().attrState.lightmapUV != null ? entry.getKey().attrState.lightmapUV : light;
            int resultOverlay = entry.getKey().attrState.overlayUV != null ? AttrUtil.exchangeLightmapUVBits(entry.getKey().attrState.overlayUV) : overlay;

                /*
                if (Objects.equals(entry.getKey().shaderName, "rendertype_entity_translucent_cull") && (resultColor & 0xFF) != 0xFF) {
                    // TEMP WORKAROUND: Depth sorting breaks
                    // ... I totally forgot what I thought about at 7/29, what leaded to "Depth sorting breaks"?
                    continue;
                }
                */

            Matrix4f resultMatrix = matrix;
                /*if (entry.getKey().useMatixProcess()) {
                    resultMatrix = matrix.copy();
                    AttrUtil.zeroRotation(resultMatrix);
                }*/

            entry.getValue().writeBlazeBuffer(vertexConsumers.getBuffer(renderType, entry.getKey().translucent),
                    resultMatrix, resultColor, resultLight, resultOverlay, drawContext);
        }
    }

    public RawModel copy() {
        this.waitForLoad();

        RawModel result = new RawModel();
        result.sourceLocation = this.sourceLocation;
        for (RawMesh mesh : this.meshList.values()) {
            RawMesh meshCopy = mesh.copy();
            result.meshList.put(meshCopy.materialProp, meshCopy);
        }
        return result;
    }

    public RawModel copyForMaterialChanges() {
        this.waitForLoad();

        RawModel result = new RawModel();
        result.sourceLocation = this.sourceLocation;
        for (RawMesh mesh : this.meshList.values()) {
            RawMesh meshCopy = mesh.copyForMaterialChanges();
            result.meshList.put(meshCopy.materialProp, meshCopy);
        }
        return result;
    }

    public void serializeTo(DataOutputStream dos) throws IOException {
        this.waitForLoad();

        dos.writeInt(meshList.size());
        for (RawMesh mesh : meshList.values()) {
            mesh.serializeTo(dos);
        }
    }

    public void setMatixProcess(Function<Matrix4f, Matrix4f> matrixProcess) {
        this.submitPostLoadTask(() -> {
            for (RawMesh mesh : meshList.values()) {
                mesh.setMatixProcess(matrixProcess);
            }
        });
    }

    public HashMap<MaterialProp, RawMesh> getMeshList() {
        this.waitForLoad();
        return meshList;
    }
}
