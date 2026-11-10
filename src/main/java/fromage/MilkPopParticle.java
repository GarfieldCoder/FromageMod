package fromage;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

final class MilkPopParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    MilkPopParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        hasPhysics = false;
        lifetime = 6;
        quadSize = .10f + level.random.nextFloat() * .05f;
        yd = .012 + level.random.nextDouble() * .012;
        alpha = 1f;
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        move(0, yd, 0);
        yd *= .6;
        setSpriteFromAge(sprites);
        if (age > 3) setAlpha((lifetime - age) / 3f);
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
