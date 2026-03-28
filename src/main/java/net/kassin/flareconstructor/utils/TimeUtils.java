package net.kassin.flareconstructor.utils;

public class TimeUtils {

    public static String getEstimatedTime(int totalBlocks, int baseBlocks, int agents, int blocksPerStrike) {

        // Fase 1 (Tudo) + Fase 2 (Tudo MENOS a base)
        int totalOperations = totalBlocks + (totalBlocks - baseBlocks);

        double cycleSeconds = 5.75;

        int teamBlocksPerCycle = agents * blocksPerStrike;
        double totalCycles = Math.ceil((double) totalOperations / teamBlocksPerCycle);
        int totalSeconds = (int) (totalCycles * cycleSeconds);

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

}