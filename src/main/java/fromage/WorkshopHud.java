package fromage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "fromage", value = Dist.CLIENT)
final class WorkshopHud {
    @SubscribeEvent
    static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()
                || minecraft.screen != null || minecraft.level == null || minecraft.player == null
                || minecraft.player.isSpectator() || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return;
        var queue = VatMotion.active(minecraft.level).get(hit.getBlockPos());
        var playback = queue == null ? null : queue.peekFirst();
        Component step = playback != null && playback.end() > minecraft.level.getGameTime()
            ? Component.translatable("message.fromage.motion." + switch (playback.motion()) {
                case STIR -> "stir";
                case CUT -> "cut";
                case LIFT -> "lift";
            })
            : WorkshopStep.forTarget(minecraft.level, minecraft.level.getBlockState(hit.getBlockPos()), minecraft.player, hit);
        if (step == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        var lines = minecraft.font.split(step, gui.guiWidth() - 20);
        int center = gui.guiWidth() / 2;
        int y = gui.guiHeight() - 62 - (lines.size() - 1) * 11;
        for (var line : lines) {
            int width = minecraft.font.width(line);
            int left = center - width / 2;
            gui.fill(left - 4, y - 2, left + width + 4, y + 10, 0xB0201812);
            gui.drawString(minecraft.font, line, left, y, 0xFFF8E8CE);
            y += 11;
        }
    }
}
