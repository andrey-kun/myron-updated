package dev.monarkhēs.myron.impl.client.obj;

import com.google.gson.*;
import dev.monarkhēs.myron.impl.client.Myron;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class ObjLoader extends AbstractObjLoader implements ModelResourceProvider, ModelVariantProvider {
    private static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(ModelTransformation.class, new ModelTransformDeserializer())
            .registerTypeAdapter(Transformation.class, new TransformDeserializer())
            .create();

    private final ResourceManager resourceManager;

    public ObjLoader(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    @Override
    public @Nullable UnbakedModel loadModelResource(Identifier identifier, ModelProviderContext modelProviderContext) {
        return loadModel(this.resourceManager, identifier, ModelTransformation.NONE, true);
    }

    @Override
    public @Nullable UnbakedModel loadModelVariant(ModelIdentifier modelIdentifier, ModelProviderContext modelProviderContext) {
        Identifier resource = new Identifier(
                modelIdentifier.getNamespace(),
                "models/item/" + modelIdentifier.getPath () + ".json");


        if (!modelIdentifier.getVariant().equals("inventory") || !this.resourceManager.containsResource(resource)) {
            return null;
        }

        try (Reader reader = new InputStreamReader(this.resourceManager.getResource(resource).getInputStream())) {
            JsonObject rawModel = JsonHelper.deserialize(reader);

            JsonElement parent = rawModel.get("parent");
            if ((!(parent instanceof JsonPrimitive) || !((JsonPrimitive) parent).isString() || !parent.getAsString().endsWith(".obj"))) {
                return null;
            }

            Identifier parentPath = new Identifier(parent.getAsString());
            ModelTransformation transformation = ModelTransformation.NONE;

            if (rawModel.has("display")) {
                JsonObject rawTransform = JsonHelper.getObject(rawModel, "display");
                transformation = GSON.fromJson(rawTransform, ModelTransformation.class);
            }

            boolean isSideLit = true;

            if (rawModel.has("gui_light")) {
                isSideLit = JsonHelper.getString(rawModel, "gui_light").equals("side");
            }

            return this.loadModel(this.resourceManager, parentPath, transformation, isSideLit);
        } catch (IOException e) {
            Myron.LOGGER.warn("Failed to load model {}:\n{}", resource, e.getMessage());
            return null;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class ModelTransformDeserializer extends ModelTransformation.Deserializer {
        public ModelTransformDeserializer() {
            super();
        }
    }
    @Environment(EnvType.CLIENT)
    public static class TransformDeserializer extends Transformation.Deserializer {
        public TransformDeserializer() {
            super();
        }
    }
}
