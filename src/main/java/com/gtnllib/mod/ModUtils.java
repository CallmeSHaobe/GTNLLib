package com.gtnllib.mod;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;

public class ModUtils {

    private static final ConcurrentMap<String, Boolean> LOADED_MODS = new ConcurrentHashMap<>();

    public static boolean isModLoaded(String modId) {
        if (modId == null || modId.isEmpty()) return false;
        Boolean loaded = LOADED_MODS.get(modId);
        if (loaded != null) return loaded;
        if (!Loader.instance()
            .hasReachedState(LoaderState.CONSTRUCTING)) return false;
        return LOADED_MODS.computeIfAbsent(modId, Loader::isModLoaded);
    }

    public static boolean areAllModsLoaded(String... modIds) {
        for (String modId : modIds) {
            if (!isModLoaded(modId)) return false;
        }
        return true;
    }

    public static boolean isAnyModLoaded(String... modIds) {
        for (String modId : modIds) {
            if (isModLoaded(modId)) return true;
        }
        return false;
    }
}
