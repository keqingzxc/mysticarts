package com.mysticarts.magic;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Rolling 10-second history of each player's state; used by the Eye of Agamotto. */
public final class TimeHistory {
    private TimeHistory() {}

    public record Snapshot(ResourceKey<Level> dim, double x, double y, double z,
                           float yaw, float pitch, float health, int food) {}

    private static final int MAX_ENTRIES = 40; // recorded every 5 ticks -> 10 s
    private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();

    public static void record(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return;
        ArrayDeque<Snapshot> q = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        q.addLast(new Snapshot(p.level().dimension(), p.getX(), p.getY(), p.getZ(),
                p.getYRot(), p.getXRot(), p.getHealth(), p.getFoodData().getFoodLevel()));
        while (q.size() > MAX_ENTRIES) q.removeFirst();
    }

    /** The snapshot from roughly 6 seconds ago (or the oldest available if history is short). */
    @Nullable
    public static Snapshot pick(ServerPlayer p) {
        ArrayDeque<Snapshot> q = HISTORY.get(p.getUUID());
        if (q == null || q.size() < 4) return null;
        List<Snapshot> list = new ArrayList<>(q);
        return list.get(Math.max(0, list.size() - 25));
    }

    public static void clear(UUID id) { HISTORY.remove(id); }

    public static void clearAll() { HISTORY.clear(); }
}
