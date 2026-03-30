package net.kassin.flareconstructor.orchestrator.steps;

import com.sk89q.worldedit.math.BlockVector3;
import net.flareplugins.core.FlareCorePlugin;
import net.kassin.flareconstructor.orchestrator.ConstructionContext;
import net.kassin.flareconstructor.orchestrator.ConstructionStep;
import net.kassin.flareconstructor.protection.WorksiteBounds;
import net.kassin.flareconstructor.protection.WorksiteTracker;
import net.kassin.flareconstructor.schematic.SchematicLayerBuilder;
import net.kassin.flareconstructor.schematic.SchematicLoader;
import net.kassin.flareconstructor.schematic.SchematicRemapper;
import net.kassin.flareconstructor.schematic.blocks.TreeDetector;
import net.kassin.flareconstructor.utils.LocationUtils;
import org.bukkit.Location;

import java.util.concurrent.CompletableFuture;

public class PreparationStep implements ConstructionStep {

    private final SchematicLoader schematicLoader;
    private final WorksiteTracker worksiteTracker;

    public PreparationStep(SchematicLoader schematicLoader, WorksiteTracker worksiteTracker) {
        this.schematicLoader = schematicLoader;
        this.worksiteTracker = worksiteTracker;
    }

    @Override
    public CompletableFuture<Void> execute(ConstructionContext context) {
        context.getSession().setCenter(context.getOrigin());

        return FlareCorePlugin.getAPI().getAsyncAPI().supply(() ->
                        schematicLoader.load(context.getBuildData().id()))
                .thenApply(clipboard -> {

                    if (!context.getBuildData().replacements().isEmpty()) {
                        SchematicRemapper.remap(clipboard, context.getBuildData().replacements());
                    }

                    com.sk89q.worldedit.regions.Region region = clipboard.getRegion();

                    int minX = region.getMinimumPoint().x();
                    int maxX = region.getMaximumPoint().x();
                    int minZ = region.getMinimumPoint().z();
                    int maxZ = region.getMaximumPoint().z();
                    int bottomY = region.getMinimumPoint().y();

                    clipboard.setOrigin(BlockVector3.at((minX + maxX) / 2, bottomY, (minZ + maxZ) / 2));

                    Location pasteLocation = context.getOrigin().getBlock().getLocation();
                    int gap = 10;
                    switch (LocationUtils.getCardinal(context.getOrigin().getYaw())) {
                        case WEST -> pasteLocation.add(-gap, 0, 0);
                        case NORTH -> pasteLocation.add(0, 0, -gap);
                        case EAST -> pasteLocation.add(gap, 0, 0);
                        case SOUTH -> pasteLocation.add(0, 0, gap);
                    }

                    WorksiteBounds bounds = context.getSession().getWorksiteBounds();
                    if (bounds != null) {
                        worksiteTracker.trackWorksite(bounds);
                    }

                    return SchematicLayerBuilder.buildLayers(clipboard, TreeDetector.detect(clipboard), pasteLocation);
                })
                .thenAccept(context::setLayers);
    }
}