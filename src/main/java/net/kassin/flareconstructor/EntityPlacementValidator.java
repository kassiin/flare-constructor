package net.kassin.flareconstructor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public class EntityPlacementValidator {

    /**
     * Verifica se a área está livre para colocar a entidade estática.
     *
     * @param clickedBlock O bloco que o jogador clicou.
     * @param player       O jogador (necessário para saber a direção/rotação).
     * @return true se a área estiver 100% livre de blocos sólidos.
     */
    public static boolean isAreaClear(Block clickedBlock, Player player) {
        // 1. Define a origem da entidade (Centro do bloco + 1 de altura)
        double originX = clickedBlock.getX() + 0.5;
        double originY = clickedBlock.getY() + 1.0;
        double originZ = clickedBlock.getZ() + 0.5;

        // Pegamos a direção que o jogador está olhando para saber o que é "trás" e "lados"
        BlockFace facing = getCardinalDirection(player);

        // 2. Definimos os limites da caixa (AABB)
        // Largura: 2 blocos total (1 para cada lado do centro)
        // Altura: 2 blocos total (O bloco atual Y, e o bloco de cima Y+1)
        double minY = originY;
        double maxY = originY + 2.0;

        double minX = originX, maxX = originX;
        double minZ = originZ, maxZ = originZ;

        // 3. Aplicamos a rotação baseada para onde o jogador olha
        if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
            // Se olha para o Norte/Sul, os "lados" estão no eixo X
            minX -= 1.0; 
            maxX += 1.0;
            
            // "Para trás 1 bloco"
            if (facing == BlockFace.NORTH) { // Norte é -Z, então trás é +Z
                maxZ += 1.0; 
            } else { // Sul é +Z, então trás é -Z
                minZ -= 1.0;
            }
        } 
        else if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
            // Se olha para Leste/Oeste, os "lados" estão no eixo Z
            minZ -= 1.0;
            maxZ += 1.0;

            // "Para trás 1 bloco"
            if (facing == BlockFace.EAST) { // Leste é +X, então trás é -X
                minX -= 1.0;
            } else { // Oeste é -X, então trás é +X
                maxX += 1.0;
            }
        }

        // 4. Criamos a caixa matemática
        BoundingBox entityBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);

        // 5. Verificamos TODOS os blocos que essa caixa toca
        // Usamos Math.floor para converter as coordenadas decimais da caixa de volta para a grade de blocos
        int bMinX = (int) Math.floor(entityBox.getMinX());
        int bMaxX = (int) Math.floor(entityBox.getMaxX() - 0.0001); // -0.0001 evita checar o bloco adjacente se a caixa terminar exatamente na borda
        int bMinY = (int) Math.floor(entityBox.getMinY());
        int bMaxY = (int) Math.floor(entityBox.getMaxY() - 0.0001);
        int bMinZ = (int) Math.floor(entityBox.getMinZ());
        int bMaxZ = (int) Math.floor(entityBox.getMaxZ() - 0.0001);

        org.bukkit.World world = clickedBlock.getWorld();

        for (int x = bMinX; x <= bMaxX; x++) {
            for (int y = bMinY; y <= bMaxY; y++) {
                for (int z = bMinZ; z <= bMaxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    
                    // Se o bloco não for passável (ar, água, grama alta), a área não está limpa
                    if (!block.isPassable()) {
                        return false; 
                    }
                }
            }
        }

        return true; // A área está perfeitamente limpa!
    }

    /**
     * Utilitário para pegar a direção cardinal pura do jogador (Ignora diagonais)
     */
    private static BlockFace getCardinalDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        yaw %= 360;
        
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }
}