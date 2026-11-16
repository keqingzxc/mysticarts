package com.mysticarts.client;

import com.mysticarts.config.ClientConfig;
import com.mysticarts.fx.ParticleKit;
import com.mysticarts.network.FxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Draws effects described by {@link FxPacket}. Everything here runs on the client, so the player's
 * own quality setting decides how dense an effect is, and effects can unfold over a few ticks:
 * circles are drawn around, spirals climb, bolts travel from caster to target.
 */
public final class ClientFx {
    private ClientFx() {}

    private record Spawn(ParticleOptions p, double x, double y, double z, double vx, double vy, double vz) {}

    private static final class Sequence {
        final List<Spawn> list;
        final int perTick;
        int index;

        Sequence(List<Spawn> list, int perTick) {
            this.list = list;
            this.perTick = Math.max(1, perTick);
        }

        void step(ClientLevel level) {
            int end = Math.min(list.size(), index + perTick);
            for (; index < end; index++) {
                Spawn s = list.get(index);
                level.addParticle(s.p(), s.x(), s.y(), s.z(), s.vx(), s.vy(), s.vz());
            }
        }

        boolean done() { return index >= list.size(); }
    }

    private static final List<Sequence> ACTIVE = new ArrayList<>();

    /** Called every client tick. */
    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        Iterator<Sequence> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Sequence s = it.next();
            s.step(level);
            if (s.done()) it.remove();
        }
    }

    public static void play(FxPacket pk) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        double f = ClientConfig.density();
        ParticleOptions p = ParticleKit.byId(pk.p1);
        ParticleOptions streak = ClientConfig.STREAKS.get() ? ParticleKit.byId(ParticleKit.streakId(pk.p1)) : p;
        RandomSource r = level.random;
        Vec3 a = new Vec3(pk.x, pk.y, pk.z);
        Vec3 b = new Vec3(pk.x2, pk.y2, pk.z2);
        List<Spawn> out = new ArrayList<>();
        int sweep = 1; // ticks over which the effect is revealed (1 = all at once)

        switch (pk.kind) {
            case FxPacket.EMIT -> out.add(new Spawn(p, a.x, a.y, a.z, b.x, b.y, b.z));

            case FxPacket.SHOCKWAVE -> {
                int n = Math.max(10, (int) Math.round(pk.n * f));
                for (int i = 0; i < n; i++) {
                    double ang = 2 * Math.PI * i / n;
                    out.add(new Spawn(streak, a.x, a.y, a.z, Math.cos(ang) * pk.f1, 0.0, Math.sin(ang) * pk.f1));
                }
            }

            case FxPacket.EXPLODE -> {
                int n = Math.max(1, (int) Math.round(pk.n * f));
                for (int i = 0; i < n; i++) {
                    Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
                    if (d.lengthSqr() < 1.0E-6) continue;
                    Vec3 v = d.normalize().scale(pk.f1 * (0.35 + 0.65 * r.nextDouble()));
                    out.add(new Spawn(i % 3 == 2 ? p : streak, a.x, a.y, a.z, v.x, v.y, v.z));
                }
            }

            case FxPacket.SWIRL -> {
                int n = Math.max(8, (int) Math.round(pk.n * f));
                for (int i = 0; i < n; i++) {
                    double ang = 2 * Math.PI * i / n;
                    out.add(new Spawn(p, a.x + Math.cos(ang) * pk.f1, a.y, a.z + Math.sin(ang) * pk.f1,
                            -Math.sin(ang) * pk.f2, 0.01, Math.cos(ang) * pk.f2));
                }
            }

            case FxPacket.PORTAL -> {
                Vec3 nrm = b.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : b.normalize();
                Vec3 up = Math.abs(nrm.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 u = nrm.cross(up).normalize();
                Vec3 v = nrm.cross(u).normalize();
                int pts = Math.max(10, (int) Math.round(pk.n * (0.5 + 0.5 * f)));
                for (int i = 0; i < pts; i++) {
                    double ang = 2 * Math.PI * i / pts;
                    Vec3 radial = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
                    Vec3 tangent = u.scale(-Math.sin(ang)).add(v.scale(Math.cos(ang))).scale(0.10);
                    Vec3 pos = a.add(radial.scale(pk.f1));
                    out.add(new Spawn(p, pos.x, pos.y, pos.z, tangent.x, tangent.y, tangent.z));
                }
                sweep = 4; // the ring is drawn around over four ticks
            }

            case FxPacket.CIRCLE -> {
                ParticleOptions rune = ParticleKit.byId(pk.p2);
                double radius = pk.f1, rot = pk.f2;
                int ringN = Math.max(14, (int) (radius * 16 * f));
                for (int i = 0; i < ringN; i++) {
                    double ang = 2 * Math.PI * i / ringN;
                    out.add(new Spawn(p, a.x + Math.cos(ang) * radius, a.y, a.z + Math.sin(ang) * radius, 0, 0, 0));
                }
                double hr = radius * 0.92;
                Vec3[] hex = new Vec3[6];
                for (int i = 0; i < 6; i++) {
                    double ang = rot + Math.PI / 3 * i;
                    hex[i] = a.add(Math.cos(ang) * hr, 0, Math.sin(ang) * hr);
                }
                for (int tri = 0; tri < 2; tri++) {
                    for (int e = 0; e < 3; e++) {
                        lineDots(out, p, hex[(tri + e * 2) % 6], hex[(tri + (e + 1) * 2) % 6], 0.5 / f);
                    }
                }
                for (int i = 0; i < 6; i++) {
                    double ang = rot + Math.PI / 6 + Math.PI / 3 * i;
                    out.add(new Spawn(rune, a.x + Math.cos(ang) * radius * 0.62, a.y + 0.08, a.z + Math.sin(ang) * radius * 0.62, 0, 0.01, 0));
                }
                out.add(new Spawn(rune, a.x, a.y + 0.1, a.z, 0, 0.01, 0));
                sweep = 8; // sketched out around the circle in a third of a second
            }

            case FxPacket.HELIX -> {
                double height = pk.f1, radius = pk.f2, turns = b.x;
                int steps = Math.max(8, (int) (height * 10 * f));
                for (int i = 0; i < steps; i++) {
                    double t = i / (double) steps;
                    double ang = t * turns * 2 * Math.PI;
                    out.add(new Spawn(p, a.x + Math.cos(ang) * radius, a.y + t * height, a.z + Math.sin(ang) * radius, 0, 0.03, 0));
                }
                sweep = 8; // the spiral climbs
            }

            case FxPacket.BEAM -> lineDots(out, p, a, b, 0.3 / f);

            case FxPacket.LIGHTNING -> {
                Vec3 delta = b.subtract(a);
                double len = delta.length();
                if (len >= 0.2) {
                    int segs = Math.max(2, (int) (len / 0.8));
                    Vec3 prev = a;
                    for (int s = 1; s <= segs; s++) {
                        Vec3 base = a.add(delta.scale(s / (double) segs));
                        Vec3 cur = s == segs ? base : base.add((r.nextDouble() - 0.5) * pk.f1,
                                (r.nextDouble() - 0.5) * pk.f1, (r.nextDouble() - 0.5) * pk.f1);
                        lineDots(out, p, prev, cur, 0.3 / f);
                        prev = cur;
                    }
                }
                sweep = 3; // the bolt travels from the caster to the target
            }

            case FxPacket.STREAM -> {
                Vec3 delta = b.subtract(a);
                double len = delta.length();
                if (len >= 0.2) {
                    Vec3 dir = delta.normalize();
                    double spacing = 0.54 / f;
                    for (double t = 0; t < len; t += spacing) {
                        Vec3 pos = a.add(dir.scale(t));
                        out.add(new Spawn(streak, pos.x, pos.y, pos.z, dir.x * pk.f1, dir.y * pk.f1, dir.z * pk.f1));
                    }
                }
            }

            default -> { return; }
        }

        if (out.isEmpty()) return;
        Sequence seq = new Sequence(out, (int) Math.ceil(out.size() / (double) Math.max(1, sweep)));
        seq.step(level);
        if (!seq.done()) ACTIVE.add(seq);
    }

    private static void lineDots(List<Spawn> out, ParticleOptions p, Vec3 from, Vec3 to, double spacing) {
        double len = from.distanceTo(to);
        if (len < 0.1) return;
        Vec3 dir = to.subtract(from).normalize();
        for (double d = 0; d < len; d += spacing) {
            Vec3 pos = from.add(dir.scale(d));
            out.add(new Spawn(p, pos.x, pos.y, pos.z, 0, 0, 0));
        }
    }
}
