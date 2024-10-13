package de.nick1st.logging.filter;

import com.google.gson.Gson;
import de.nick1st.logging.filter.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class CommonClass {
    private static final File CONFIG_FILE = new File(Services.PLATFORM.getConfigFolder().toFile(), "log_filter.json");
    private static final FileWatcher configWatcher = new FileWatcher(CONFIG_FILE, () -> {
        Constants.LOG.info("Reloading log filter config.");
        applyConfig();
    });
    private static final Filter FILTER = new Filter();

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {
        Constants.LOG.warn("Log Filter was loaded on {}! Log messages might be removed!", Services.PLATFORM.getPlatformName());
        try {
            ((Logger) LogManager.getRootLogger()).addFilter(FILTER);
            applyConfig();
        } catch (ClassCastException e) {
            Constants.LOG.error("Root logger type unknown, can't add filter!");
        }
        configWatcher.setDaemon(true);
        configWatcher.start();
    }

    private static void applyConfig() {
        try {
            Config config = new Gson().fromJson(new FileReader(CONFIG_FILE), Config.class);
            FILTER.debugLevel = config.logEvents;
            List<FilterPredicate> filters = new ArrayList<>();
            for (Config.FilterRule rule : config.rules) {
                filters.add(new FilterPredicate(rule));
            }
            FILTER.filters = filters;
        } catch (FileNotFoundException e) {
            Constants.LOG.error("Config File not found, can't add filters!");
        } catch (Exception e) {
            Constants.LOG.error("Error while loading config, won't update!", e);
        }
    }
}