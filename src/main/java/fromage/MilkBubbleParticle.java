package fromage;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

final class MilkBubbleParticle extends TextureSheetParticle {
    MilkBubbleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        hasPhysics = false;
        lifetime = 10 + level.random.nextInt(6);
        quadSize = .035f + level.random.nextFloat() * .03f;
        xd = (level.random.nextDouble() - .5) * .006;
        yd = .012 + level.random.nextDouble() * .012;
        zd = (level.random.nextDouble() - .5) * .006;
        alpha = .9f;
        pickSprite(sprites);
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        yd += .0003;
        xd += (random.nextDouble() - .5) * .001;
        zd += (random.nextDouble() - .5) * .001;
        move(xd, yd, zd);
        xd *= .98;
        zd *= .98;
        if (age > lifetime - 5) setAlpha((lifetime - age) / 5f * .9f);
    }

    @Override public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
