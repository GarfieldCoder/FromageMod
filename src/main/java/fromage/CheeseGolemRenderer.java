package fromage;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "fromage", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class CheeseGolemRenderer extends MobRenderer<CheeseGolem, CheeseGolemModel> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("fromage", "textures/entity/cheese_golem.png");

    CheeseGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new CheeseGolemModel(context.bakeLayer(CheeseGolemModel.LAYER)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(CheeseGolem entity) {
        return TEXTURE;
    }

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CheeseGolemModel.LAYER, CheeseGolemModel::createBodyLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Fromage.CHEESE_GOLEM.get(), CheeseGolemRenderer::new);
    }
}
