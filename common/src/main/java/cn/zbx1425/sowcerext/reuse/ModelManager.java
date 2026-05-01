package cn.zbx1425.sowcerext.reuse;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.sowcer.model.Mesh;
import cn.zbx1425.sowcer.model.Model;
import cn.zbx1425.sowcer.model.VertArrays;
import cn.zbx1425.sowcer.object.VertArray;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import cn.zbx1425.sowcer.vertex.VertAttrSrc;
import cn.zbx1425.sowcer.vertex.VertAttrType;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawMesh;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.loader.CsvModelLoader;
import cn.zbx1425.sowcerext.model.loader.NmbModelLoader;
import cn.zbx1425.sowcerext.model.loader.ObjModelLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ModelManager {
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();

    public HashMap<ResourceLocation, Model> uploadedModels = new HashMap<>();
    public HashMap<ResourceLocation, ModelCluster> uploadedVertArrays = new HashMap<>();
    public HashMap<ResourceLocation, RawModel> loadedRawModels = new HashMap<>();

    public int vaoCount, vboCount;

    public static final VertAttrMapping DEFAULT_MAPPING = new VertAttrMapping.Builder()
            .set(VertAttrType.POSITION, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.COLOR, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_TEXTURE, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.UV_OVERLAY, VertAttrSrc.GLOBAL)
            .set(VertAttrType.UV_LIGHTMAP, VertAttrSrc.GLOBAL)
            .set(VertAttrType.NORMAL, VertAttrSrc.VERTEX_BUF)
            .set(VertAttrType.MATRIX_MODEL, VertAttrSrc.GLOBAL)
            .build();

    public void clear() {
        vaoCount = 0;
        vboCount = 0;

        for (ModelCluster vertArrays : uploadedVertArrays.values()) {
            vertArrays.close();
        }
        uploadedVertArrays.clear();

        for (Model model : uploadedModels.values()) {
            model.close();
        }
        uploadedModels.clear();

        loadedRawModels.clear();
    }

    public void clearNamespace(String namespace) {
        uploadedVertArrays.entrySet().stream()
                .filter(k -> k.getKey().getNamespace().equals(namespace))
                .forEach(k -> {
                    vaoCount -= k.getValue().uploadedOpaqueParts == null ? 0 : k.getValue().uploadedOpaqueParts.meshList.size();
                    k.getValue().close();
                });

        uploadedVertArrays.keySet().removeIf(k -> k.getNamespace().equals(namespace));

        uploadedModels.entrySet().stream()
                .filter(k -> k.getKey().getNamespace().equals(namespace))
                .forEach(k -> {
                    vboCount -= k.getValue().meshList.size();
                    k.getValue().close();
                });

        uploadedModels.keySet().removeIf(k -> k.getNamespace().equals(namespace));

        loadedRawModels.keySet().removeIf(k -> k.getNamespace().equals(namespace));
    }

    public RawModel loadRawModel(ResourceManager resourceManager, ResourceLocation objLocation, AtlasManager atlasManager) throws IOException {
        var existing = loadedRawModels.get(objLocation);

        if (existing != null) {
            return existing;
        }

        var fileType = FilenameUtils.getExtension(objLocation.getPath());

        return switch (fileType) {
            case "obj" -> ObjModelLoader.loadModel(resourceManager, objLocation, atlasManager);
            case "csv" -> CsvModelLoader.loadModel(resourceManager, objLocation, atlasManager);
            case "nmb" -> NmbModelLoader.loadModel(resourceManager, objLocation, atlasManager);
            default -> throw new IllegalArgumentException("Model cannot be loaded as a raw model: " + objLocation);
        };
    }

    public Map<String, RawModel> loadPartedRawModel(ResourceManager resourceManager, ResourceLocation objLocation, AtlasManager atlasManager) throws IOException {
        String fileType = FilenameUtils.getExtension(objLocation.getPath());

        return switch (fileType) {
            case "obj" -> ObjModelLoader.loadModels(resourceManager, objLocation, atlasManager);
            default -> throw new IllegalArgumentException("Model cannot be loaded as a parted raw model: " + resourceManager);
        };
    }

    public Model uploadModel(RawModel rawModel) {
        var result = new Model();

        ResourceLocation id;

        if (rawModel.sourceLocation != null) {
            var existing = uploadedModels.get(rawModel.sourceLocation);

            if (existing != null) {
                return existing;
            }

            id = rawModel.sourceLocation;
        } else {
            id = new ResourceLocation("sowcerext-anonymous:model/" + UUID.randomUUID());
        }

        uploadedModels.put(id, result);

        uploadExecutor.submit(() -> {
            rawModel.waitForLoad();

            var renderThreadTask = rawModel.uploadAsync(DEFAULT_MAPPING);

            RenderSystem.recordRenderCall(() -> {
                var uploaded = renderThreadTask.get();

                synchronized (result) {
                    result.meshList.addAll(uploaded.meshList);
                }

                synchronized (this) {
                    vaoCount += uploaded.meshList.size();
                }
            });
        });

        return result;
    }

    public ModelCluster uploadVertArrays(RawModel rawModel) {
        var result = new ModelCluster();

        ResourceLocation id;

        if (rawModel.sourceLocation != null) {
            var existing = uploadedVertArrays.get(rawModel.sourceLocation);

            if (existing != null) {
                return existing;
            }

            id = rawModel.sourceLocation;
        } else {
            id = new ResourceLocation("sowcerext-anonymous:vertarrays/" + UUID.randomUUID());
        }

        uploadedVertArrays.put(id, result);

        result.uploadTask = uploadExecutor.submit(() -> {
            rawModel.waitForLoad();

            synchronized (result) {
                for (RawMesh mesh : rawModel.getMeshList().values()) {
                    if (mesh.materialProp.translucent) {
                        result.translucentParts.append(mesh);
                    } else {
                        result.opaqueParts.append(mesh);
                    }
                }
            }

            var renderThreadTaskTranslucent = result.translucentParts.uploadAsync(DEFAULT_MAPPING);
            var renderThreadTaskOpaque = result.opaqueParts.uploadAsync(DEFAULT_MAPPING);

            RenderSystem.recordRenderCall(() -> {
                var uploadedTranslucent = renderThreadTaskTranslucent.get();
                var uploadedOpaque = renderThreadTaskOpaque.get();

                synchronized (result) {
                    GlStateTracker.capture();

                    for (Mesh mesh : uploadedTranslucent.meshList) {
                        VertArray meshVertArray = new VertArray();
                        meshVertArray.create(mesh, DEFAULT_MAPPING, null);
                        result.uploadedTranslucentParts.meshList.add(meshVertArray);
                    }

                    for (Mesh mesh : uploadedOpaque.meshList) {
                        VertArray meshVertArray = new VertArray();
                        meshVertArray.create(mesh, DEFAULT_MAPPING, null);
                        result.uploadedOpaqueParts.meshList.add(meshVertArray);
                    }

                    GlStateTracker.restore();
                }

                synchronized (this) {
                    vaoCount += uploadedOpaque.meshList.size();
                }

                synchronized (result) {
                    for (var task : result.postUploadTasks) {
                        task.run();
                    }

                    result.postUploadTasks.clear();

                    result.uploadTask = null;
                }
            });
        });

        return result;
    }
}
