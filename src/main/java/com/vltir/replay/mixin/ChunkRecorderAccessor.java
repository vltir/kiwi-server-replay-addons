package com.vltir.replay.mixin;

import com.vltir.replay.addons.ChunkRecorderPresenceAddon;
import me.senseiwells.replay.chunk.ChunkRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private timestamp-tracking fields of {@link ChunkRecorder} so that
 * {@link ChunkRecorderPresenceAddon} can implement its own
 * pause/resume logic (skip recording time when no real players are in the area).
 *
 * <p>The underlying mechanism in {@code ChunkRecorder} tracks paused time via two fields:</p>
 * <ul>
 *   <li>{@code lastPaused} – non-zero while the recorder is paused (epoch-millis timestamp)</li>
 *   <li>{@code totalPausedTime} – cumulative milliseconds already spent paused</li>
 * </ul>
 * <p>Both are subtracted from the replay timestamp in {@code ChunkRecorder#getTimestamp()},
 * so manipulating them causes the replay to skip the paused interval.</p>
 */
@Mixin(value = ChunkRecorder.class, remap = false)
public interface ChunkRecorderAccessor {

    /** Returns the epoch-millis timestamp at which the recorder was last paused, or {@code 0} if not paused. */
    @Accessor("lastPaused")
    long kiwiServerReplayAddons$getLastPaused();

    /** Sets the epoch-millis timestamp used to mark the recorder as paused ({@code 0} = not paused). */
    @Accessor("lastPaused")
    void kiwiServerReplayAddons$setLastPaused(long value);

    /** Returns the total milliseconds already accumulated as paused time. */
    @Accessor("totalPausedTime")
    long kiwiServerReplayAddons$getTotalPausedTime();

    /** Sets the total accumulated paused time in milliseconds. */
    @Accessor("totalPausedTime")
    void kiwiServerReplayAddons$setTotalPausedTime(long value);
}
