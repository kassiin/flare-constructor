package net.kassin.flareconstructor.schematic.blocks;

import java.util.List;

public record Layers(List<Layer> phase1, List<Layer> phase2) {
    public record Layer(List<BlockEntry> blocks) {
        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }
}
