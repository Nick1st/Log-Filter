package de.nick1st.logging.filter;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class LogFilterModNeo {

    public LogFilterModNeo(IEventBus eventBus) {
        // Use NeoForge to bootstrap the Common mod.
        CommonClass.init();
    }
}