package net.kassin.flareconstructor.menu.configuration;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConstructionRegistry {

    private final Set<String> registeredBuilds = new HashSet<>();

    public void load(FileConfiguration config) {
        registeredBuilds.clear();
        List<String> builds = config.getStringList("builds");

        for (String id : builds) {
            registeredBuilds.add(id.toLowerCase());
        }

    }

    public Set<String> getAvailableBuildIds() {
        return Collections.unmodifiableSet(registeredBuilds);
    }

    public boolean isRegistered(String id) {
        return registeredBuilds.contains(id.toLowerCase());
    }

    public String getBuild(String id) {
        return registeredBuilds.contains(id.toLowerCase()) ? id : "";
    }

}