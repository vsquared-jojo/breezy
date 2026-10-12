package com.example.windfx;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A thin, tapered white streak that flows along the wind and gently curves,
 * always rotated on screen to point along its direction of travel.
 */
public class WindStreakParticle extends SingleQuadParticle {

    private final float baseAlpha;
    private final float turnRate;
    private final float phase;
    private final double bob;

    protected WindStreakParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        RandomSource r = level.getRandom();

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;

        this.lifetime = 30 + r.nextInt(26);
        this.quadSize = (0.5F + r.nextFloat() * 0.7F) * WindFxConfig.get().lineLength;
        this.baseAlpha = 0.45F + r.nextFloat() * 0.3F;
        this.turnRate = (r.nextFloat() - 0.5F) * 0.10F;   // radians per tick, max swing
        this.phase = r.nextFloat() * 6.2831F;
        this.bob = Math.sqrt(vx * vx + vz * vz) * 0.10;

        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(0.0F);
    }

    @Override
    public void tick() {
        // curve the path sideways + a little vertical wobble (speed stays constant)
        double turn = this.turnRate * Math.sin((this.age + this.phase) * 0.18);
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        double nx = this.xd * cos - this.zd * sin;
        double nz = this.xd * sin + this.zd * cos;
        this.xd = nx;
        this.zd = nz;
        this.yd = Math.cos((this.age + this.phase) * 0.12) * this.bob;

        // fade in and out over the lifetime
        float t = Math.min(1.0F, (float) this.age / (float) this.lifetime);
        this.setAlpha(this.baseAlpha * (float) Math.sin(Math.PI * t));

        super.tick();
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        // Start from the normal camera-facing orientation...
        Quaternionf q = new Quaternionf(camera.rotation());
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(q);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(q);

        // ...then spin the quad in its own plane so the streak points along the motion on screen
        float sx = (float) this.xd * right.x + (float) this.yd * right.y + (float) this.zd * right.z;
        float sy = (float) this.xd * up.x + (float) this.yd * up.y + (float) this.zd * up.z;
        if (sx * sx + sy * sy > 1.0E-8F) {
            q.rotateZ((float) Math.atan2(sy, sx));
        }
        this.extractRotatedQuad(state, camera, q, partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public record Provider(FabricSpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource random) {
            return new WindStreakParticle(level, x, y, z, vx, vy, vz, this.sprites.get(random));
        }
    }
}
