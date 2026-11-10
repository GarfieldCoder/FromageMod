package fromage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

enum VatMotion {
    STIR(18), CUT(13), LIFT(18);

    static final int BLOCK_EVENT = 91;
    private static final Map<Level, Map<BlockPos, Deque<Playback>>> CLIENT_MOTIONS = new WeakHashMap<>();
    final int duration;

    VatMotion(int duration) {
        this.duration = duration;
    }

    static void receive(Level level, BlockPos pos, int value) {
        VatMotion[] motions = values();
        if (value < 0 || value >= motions.length) return;
        Deque<Playback> queue = CLIENT_MOTIONS.computeIfAbsent(level, ignored -> new HashMap<>())
            .computeIfAbsent(pos.immutable(), ignored -> new ArrayDeque<>());
        Playback last = queue.peekLast();
        long now = level.getGameTime();
        long start = last == null ? now : Math.max(now, last.end());
        queue.addLast(new Playback(motions[value], start));
    }

    static Map<BlockPos, Deque<Playback>> active(Level level) {
        return CLIENT_MOTIONS.getOrDefault(level, Map.of());
    }

    record Playback(VatMotion motion, long start) {
        long end() {
            return start + motion.duration;
        }
    }
}
