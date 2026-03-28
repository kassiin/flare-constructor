package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import net.kassin.flareconstructor.FlareConstructorPlugin;
import net.kassin.flareconstructor.menu.configuration.ConstructionRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SchematicLoader {

    private final List<String> EXTENSIONS = List.of(".schem", ".schematic");

    private final List<String> SEARCH_FOLDERS = List.of(
            "FlareConstructor/schematics",
            "flare-constructor/schematics",
            "WorldEdit/schematics",
            "FastAsyncWorldEdit/schematics"
    );

    private final FlareConstructorPlugin plugin;
    private final ConstructionRegistry registry;

    private final Map<String, File> fileCache = new ConcurrentHashMap<>();

    public SchematicLoader(FlareConstructorPlugin plugin, ConstructionRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /**
     * Procura no HD APENAS as schematics que foram liberadas na config,
     * ignorando se o arquivo físico está com letras Maiúsculas ou Minúsculas.
     */
    public void scanAllowedSchematics() {
        fileCache.clear();
        Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();

        for (String folderName : SEARCH_FOLDERS) {
            Path folderPath = pluginsDir.resolve(folderName);
            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) continue;

            try (Stream<Path> paths = Files.walk(folderPath, 1)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();

                    for (String ext : EXTENSIONS) {
                        if (fileName.toLowerCase().endsWith(ext)) {

                            String schemName = fileName.substring(0, fileName.length() - ext.length()).toLowerCase();

                            if (registry.isRegistered(schemName)) {
                                fileCache.putIfAbsent(schemName, path.toFile());
                            }
                            break;
                        }
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().warning("Erro ao escanear a pasta de schematics: " + folderName);
            }
        }

        plugin.getLogger().info("[Flare] Loader: " + fileCache.size() + " schematics validadas e mapeadas no HD.");
    }

    public Clipboard load(String schemName) {
        File file = fileCache.get(schemName.toLowerCase());

        if (file == null) {
            throw new IllegalArgumentException("Schematic '" + schemName + "' não está mapeada. O arquivo existe nas pastas?");
        }

        ClipboardFormat format = ClipboardFormats.findByFile(file);

        if (format == null) {
            throw new IllegalArgumentException("Formato de schematic não suportado para o arquivo: " + file.getName());
        }

        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) {
            return reader.read();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao tentar ler os dados da schematic: " + file.getName(), e);
        }
    }

}