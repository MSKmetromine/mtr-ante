package cn.zbx1425.sowcerext.model;

import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.batch.EnqueueProp;
import cn.zbx1425.sowcer.batch.ShaderProp;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.object.VertArray;
import cn.zbx1425.sowcer.util.AttrUtil;
import cn.zbx1425.sowcer.util.DrawContext;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcer.vertex.VertAttrState;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import cn.zbx1425.sowcerext.reuse.ModelManager;
import net.minecraft.resources.ResourceLocation;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public class ModelCluster implements Closeable {

    public VertArrays uploadedOpaqueParts;
    public RawModel opaqueParts;
    public VertArrays uploadedTranslucentParts;
    public RawModel translucentParts;

    public Future<?> uploadTask;

    public List<Runnable> postUploadTasks = new ArrayList<>();

    public ModelCluster(RawModel source, VertAttrMapping mapping, ModelManager modelManager) {
        this.translucentParts = new RawModel();
        this.opaqueParts = new RawModel();
        for (RawMesh mesh : source.getMeshList().values()) {
            if (mesh.materialProp.translucent) {
                translucentParts.append(mesh);
            } else {
                opaqueParts.append(mesh);
            }
        }
        this.uploadedOpaqueParts = VertArrays.createAll(
                modelManager.uploadModel(opaqueParts), mapping, null);
        this.uploadedTranslucentParts = VertArrays.createAll(
                modelManager.uploadModel(translucentParts), mapping, null);
    }

    public ModelCluster(RawModel source, VertAttrMapping mapping) {
        // Untracked variant
        this.translucentParts = new RawModel();
        this.opaqueParts = new RawModel();
        for (RawMesh mesh : source.getMeshList().values()) {
            if (mesh.materialProp.translucent) {
                translucentParts.append(mesh);
            } else {
                opaqueParts.append(mesh);
            }
        }
        this.uploadedOpaqueParts = VertArrays.createAll(
                opaqueParts.upload(mapping), mapping, null);
        this.uploadedTranslucentParts = VertArrays.createAll(
                translucentParts.upload(mapping), mapping, null);
    }

    private ModelCluster(VertArrays uploadedOpaqueParts, RawModel opaqueParts, VertArrays uploadedTranslucentParts, RawModel translucentParts) {
        this.uploadedOpaqueParts = uploadedOpaqueParts;
        this.opaqueParts = opaqueParts;
        this.uploadedTranslucentParts = uploadedTranslucentParts;
        this.translucentParts = translucentParts;
    }

    public ModelCluster() {
        this.translucentParts = new RawModel();
        this.opaqueParts = new RawModel();

        this.uploadedTranslucentParts = new VertArrays();
        this.uploadedOpaqueParts = new VertArrays();
    }

    private void submitPostUploadTask(Runnable task) {
        if (this.uploadTask == null) {
            task.run();
            return;
        }

        this.postUploadTasks.add(task);
    }

    public void enqueueOpaqueGl(BatchManager batchManager, Matrix4f pose, int light, int overlay, DrawContext drawContext) {
        int shaderLightmapUV = AttrUtil.exchangeLightmapUVBits(light);
        batchManager.enqueue(uploadedOpaqueParts, new EnqueueProp(
                new VertAttrState()
                        .setColor(255, 255, 255, 255).setOverlayUV(overlay)
                        .setLightmapUV(shaderLightmapUV).setModelMatrix(pose)
        ), ShaderProp.DEFAULT);
    }

    public void enqueueOpaqueBlaze(BufferSourceProxy vertexConsumers, Matrix4f pose, int light, int overlay, DrawContext drawContext) {
        opaqueParts.writeBlazeBuffer(vertexConsumers, pose, light, overlay, drawContext);
    }

    public void enqueueTranslucentGl(BatchManager batchManager, Matrix4f matrix4f, int light, int overlay, DrawContext drawContext) {
        int shaderLightmapUV = AttrUtil.exchangeLightmapUVBits(light);
        batchManager.enqueue(uploadedTranslucentParts, new EnqueueProp(
                new VertAttrState()
                        .setColor(255, 255, 255, 255).setOverlayUV(overlay)
                        .setLightmapUV(shaderLightmapUV).setModelMatrix(matrix4f)
        ), ShaderProp.DEFAULT);
    }

    public void enqueueTranslucentBlaze(BufferSourceProxy vertexConsumers, Matrix4f pose, int light, int overlay, DrawContext drawContext) {
        translucentParts.writeBlazeBuffer(vertexConsumers, pose, light, overlay, drawContext);
    }

    public void setMatixProcess(Function<Matrix4f, Matrix4f> matrixProcess) {
        this.submitPostUploadTask(() -> {
            opaqueParts.setMatixProcess(matrixProcess);
            translucentParts.setMatixProcess(matrixProcess);
            uploadedOpaqueParts.setMatixProcess(matrixProcess);
            uploadedTranslucentParts.setMatixProcess(matrixProcess);
        });
    }

    @Override
    public void close() {
        this.submitPostUploadTask(() -> {
            uploadedOpaqueParts.close();
            uploadedTranslucentParts.close();
        });
    }


    public void replaceTexture(String oldTexture, ResourceLocation newTexture) {
        this.submitPostUploadTask(() -> {
            uploadedOpaqueParts.replaceTexture(oldTexture, newTexture);
            opaqueParts.replaceTexture(oldTexture, newTexture);
            uploadedTranslucentParts.replaceTexture(oldTexture, newTexture);
            translucentParts.replaceTexture(oldTexture, newTexture);
        });
    }

    public void replaceAllTexture(ResourceLocation newTexture) {
        this.submitPostUploadTask(() -> {
            uploadedOpaqueParts.replaceAllTexture(newTexture);
            opaqueParts.replaceAllTexture(newTexture);
            uploadedTranslucentParts.replaceAllTexture(newTexture);
            translucentParts.replaceAllTexture(newTexture);
        });
    }

    public ModelCluster copyForMaterialChanges() {
        var cluster = new ModelCluster();

        cluster.uploadTask = this.uploadTask;

        this.submitPostUploadTask(() -> {
            cluster.uploadedOpaqueParts = this.uploadedOpaqueParts.copyForMaterialChanges();
            cluster.opaqueParts = this.opaqueParts.copyForMaterialChanges();
            cluster.uploadedTranslucentParts = this.uploadedTranslucentParts.copyForMaterialChanges();
            cluster.translucentParts = this.translucentParts.copyForMaterialChanges();

            for (var task : cluster.postUploadTasks) {
                task.run();
            }

            cluster.postUploadTasks.clear();
        });

        return cluster;
    }
}
