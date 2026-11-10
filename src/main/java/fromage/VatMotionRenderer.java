package fromage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = "fromage", value = Dist.CLIENT)
final class VatMotionRenderer {
    @SubscribeEvent
    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) return;
        var active = VatMotion.active(level);
        if (active.isEmpty()) return;

        var camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var buffers = minecraft.renderBuffers().bufferSource();
        double time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var queue = entry.getValue();
            while (!queue.isEmpty() && queue.peekFirst().end() <= time) queue.removeFirst();
            if (queue.isEmpty()) {
                iterator.remove();
                continue;
            }
            var pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof Fromage.Station station) || !station.vat) {
                iterator.remove();
                continue;
            }
            var playback = queue.getFirst();
            float progress = Mth.clamp((float) (time - playback.start()) / playback.motion().duration, 0, 1);
            double surface = (state.is(Fromage.FIREBOX.get()) ? 14.75 : state.is(Fromage.FIREBOX_BASIN.get()) ? 6.75 : 12.75) / 16.0;
            int light = LevelRenderer.getLightColor(level, pos.above());
            pose.pushPose();
            pose.translate(pos.getX() + .5 - camera.x, pos.getY() + surface - camera.y, pos.getZ() + .5 - camera.z);
            switch (playback.motion()) {
                case STIR -> stir(minecraft, pose, buffers, light, progress);
                case CUT -> cut(minecraft, pose, buffers, light, progress);
                case LIFT -> lift(minecraft, pose, buffers, light, progress);
            }
            pose.popPose();
        }
    }

    private static void stir(Minecraft minecraft, PoseStack pose, MultiBufferSource buffers, int light, float progress) {
        float phase = progress * Mth.TWO_PI;
        pose.translate(.16 * Mth.cos(phase), .025 + .15 * Math.pow(Math.cos(Math.PI * progress), 6),
            .16 * Mth.sin(phase));
        pose.mulPose(Axis.ZP.rotationDegrees(16 * Mth.cos(phase)));
        var oak = Blocks.OAK_PLANKS.defaultBlockState();
        cube(minecraft, pose, buffers, light, oak, -.025f, 0, -.025f, .05f, .46f, .05f);
        cube(minecraft, pose, buffers, light, oak, -.075f, -.025f, -.06f, .15f, .05f, .12f);
    }

    private static void cut(Minecraft minecraft, PoseStack pose, MultiBufferSource buffers, int light, float progress) {
        pose.translate(-.24 + .48 * progress, .38 - .16 * Mth.sin(progress * Mth.PI), 0);
        pose.mulPose(Axis.YP.rotationDegrees(22 + 22 * Mth.sin(Mth.PI * progress)));
        pose.mulPose(Axis.ZP.rotationDegrees(-135));
        pose.scale(.82f, .82f, .82f);
        minecraft.getItemRenderer().renderStatic(Items.IRON_SWORD.getDefaultInstance(), ItemDisplayContext.NONE, light,
            OverlayTexture.NO_OVERLAY, pose, buffers, minecraft.level, 0);
    }

    private static void lift(Minecraft minecraft, PoseStack pose, MultiBufferSource buffers, int light, float progress) {
        pose.translate(0, .04 + .42 * progress, .22 * progress);
        pose.mulPose(Axis.XP.rotationDegrees(18 * progress));
        var cloth = Blocks.WHITE_WOOL.defaultBlockState();
        cube(minecraft, pose, buffers, light, cloth, -.16f, -.055f, -.16f, .32f, .05f, .32f);
        cube(minecraft, pose, buffers, light, cloth, -.12f, -.13f, -.12f, .24f, .09f, .24f);
        cube(minecraft, pose, buffers, light, Blocks.SANDSTONE.defaultBlockState(),
            -.115f, -.015f, -.115f, .23f, .07f, .23f);
    }

    private static void cube(Minecraft minecraft, PoseStack pose, MultiBufferSource buffers, int light, BlockState state,
            float x, float y, float z, float width, float height, float depth) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(width, height, depth);
        minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
