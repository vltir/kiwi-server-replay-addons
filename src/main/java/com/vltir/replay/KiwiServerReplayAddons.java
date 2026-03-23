package com.vltir.replay;

import com.vltir.replay.addons.ChunkRecorderPresenceAddon;
import com.vltir.replay.config.KiwiAddonsConfigManager;
import net.fabricmc.api.ModInitializer;

public class KiwiServerReplayAddons implements ModInitializer {

    @Override
    public void onInitialize() {
        KiwiAddonsConfigManager.load();
        new ChunkRecorderPresenceAddon().register();
    }
}
