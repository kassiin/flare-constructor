package net.kassin.flareconstructor.orchestrator.observer;

import org.bukkit.Location;

public interface BuildObserver {
    
    void onBuildStart(Location centerLocation);
    
    void onBlocksPlaced(Location targetBlock);
    
    void onBuildComplete();
    
}