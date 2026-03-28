package net.kassin.flareconstructor.schematic.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kassin.flareconstructor.protection.WorksiteTracker;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ProjectRegistry {

    private final Cache<UUID, ConstructionProject> activeProjects;

    public ProjectRegistry(WorksiteTracker tracker) {
        this.activeProjects = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .removalListener((key, project, cause) -> {
                    if (project instanceof ConstructionProject p) {
                        p.cancel();

                        if (p.getWorksiteBounds() != null) {
                            tracker.untrackWorksite(p.getWorksiteBounds());
                        }

                    }
                })
                .build();
    }

    public ConstructionProject createProject(UUID playerId) {
        ConstructionProject project = new ConstructionProject(playerId);
        activeProjects.put(playerId, project);
        return project;
    }

    public ConstructionProject getProject(UUID playerId) {
        return activeProjects.getIfPresent(playerId);
    }

    public void removeProject(UUID playerId) {
        activeProjects.invalidate(playerId);
    }

    public boolean hasActiveProject(UUID playerId) {
        return activeProjects.asMap().containsKey(playerId);
    }

    public Collection<ConstructionProject> getAllActive() {
        return activeProjects.asMap().values();
    }
}