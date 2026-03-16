package com.vltir.replay;

import com.vltir.replay.mixin.ChunkRecorderAccessor;
import me.senseiwells.replay.chunk.ChunkRecorder;
import me.senseiwells.replay.chunk.ChunkRecorders;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Automatically pauses and resumes chunk area recordings based on whether any
 * <em>real</em> players (not Carpet bots or other fake players) are present
 * inside the recorded chunk area.
 *
 * <h3>Fake-player detection</h3>
 * The same logic used by ServerReplay's built-in {@code is_fake} predicate is
 * applied: a {@link ServerPlayer} is considered fake when its runtime class is
 * not exactly {@code ServerPlayer.class} (i.e., it is a subclass, as is the
 * case for Carpet fake-player instances).
 *
 * <h3>Pause/resume mechanism</h3>
 * {@code ChunkRecorder} tracks paused time via two private fields
 * ({@code lastPaused} and {@code totalPausedTime}) that are subtracted from the
 * replay timestamp in {@code getTimestamp()}.  The {@link ChunkRecorderAccessor}
 * mixin exposes those fields so this class can implement its own pause/resume
 * logic independently of ServerReplay's {@code skipWhenChunksUnloaded} config flag.
 *
 * <h3>Tick scheduling</h3>
 * The check runs on {@link ServerTickEvents#END_SERVER_TICK} every 20 server
 * ticks (≈ 1 second) to keep overhead low.
 */
public class ChunkRecorderPresenceAddon {

    // Accessed only from the single server tick thread — no synchronization needed.
    private int tickCounter = 0;

    /** Registers the server-tick callback. Call once from {@code KiwiServerReplayAddons#onInitialize()}. */
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        if (++tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        for (ChunkRecorder recorder : ChunkRecorders.recorders()) {
            boolean hasRealPlayer = hasRealPlayerInArea(server, recorder);
            ChunkRecorderAccessor acc = asAccessor(recorder);
            long lastPaused = acc.kiwiServerReplayAddons$getLastPaused();

            if (!hasRealPlayer && lastPaused == 0L) {
                // No real players present — pause the recorder by recording the start time.
                acc.kiwiServerReplayAddons$setLastPaused(System.currentTimeMillis());
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(String.format("Paused recording for %s", recorder.getName())), false
                );
            } else if (hasRealPlayer && lastPaused != 0L) {
                // A real player has entered (or is in) the area — resume by committing the
                // elapsed pause duration and clearing the paused marker.
                long pauseDuration = System.currentTimeMillis() - lastPaused;
                acc.kiwiServerReplayAddons$setTotalPausedTime(acc.kiwiServerReplayAddons$getTotalPausedTime() + pauseDuration);
                acc.kiwiServerReplayAddons$setLastPaused(0L);
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(String.format("Resumed recording for %s", recorder.getName())), false
                );
            }
        }
    }

    /**
     * {@link ChunkRecorder} is a final Kotlin class, so javac rejects a direct cast to the
     * accessor interface as an inconvertible type. Casting through {@link Object} matches the
     * runtime shape after Mixin applies the interface.
     */
    private static ChunkRecorderAccessor asAccessor(ChunkRecorder recorder) {
        return (ChunkRecorderAccessor) (Object) recorder;
    }

    /**
     * Returns {@code true} if at least one real player is currently inside the
     * chunk area of the given recorder.
     *
     * <p>Fake players (e.g., Carpet bots) are identified by the same runtime-class
     * check used in ServerReplay's {@code IsFakePredicate}: if
     * {@code player.getClass() != ServerPlayer.class} the player is fake and is
     * skipped.</p>
     */
    private static boolean hasRealPlayerInArea(MinecraftServer server, ChunkRecorder recorder) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Skip fake players — matches ServerReplay's is_fake predicate logic:
            // real players have exactly class ServerPlayer; subclasses are fake (e.g. Carpet bots).
            if (player.getClass() != ServerPlayer.class) {
                continue;
            }
            ChunkPos chunkPos = player.chunkPosition();
            if (recorder.getChunks().contains(player.level().dimension(), chunkPos)) {
                return true;
            }
        }
        return false;
    }
}
