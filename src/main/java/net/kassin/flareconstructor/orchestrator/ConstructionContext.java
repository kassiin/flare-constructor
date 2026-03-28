package net.kassin.flareconstructor.orchestrator;

import lombok.Data;
import net.kassin.flareconstructor.menu.configuration.BuildData;
import net.kassin.flareconstructor.orchestrator.entity.AgentController;
import net.kassin.flareconstructor.schematic.blocks.Layers;
import net.kassin.flareconstructor.schematic.engine.patrol.ConstructionBarrierFence;
import net.kassin.flareconstructor.schematic.session.ConstructionProject;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConstructionContext {
    private final BuildData buildData;
    private final Location origin;
    private final Location houseOrigin;
    private final Location pathFinderTarget;
    private final ConstructionProject session;
    private final Player player;
    private ConstructionBarrierFence barrierFence;

    private final List<AgentController> agents = new ArrayList<>();
    private Layers layers;

}