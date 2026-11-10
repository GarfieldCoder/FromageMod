package fromage;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = "fromage", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class WheyDropParticle extends TextureSheetParticle {
    private WheyDropParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        hasPhysics = false;
        lifetime = 6;
        quadSize = .075f;
        pickSprite(sprites);
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
        } else {
            move(0, -.09, 0);
            if (age >= 4) setAlpha((lifetime - age) / 2f);
        }
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(Fromage.WHEY_DROP.get(),
            sprites -> (type, level, x, y, z, dx, dy, dz) -> new WheyDropParticle(level, x, y, z, sprites));
    }
}

