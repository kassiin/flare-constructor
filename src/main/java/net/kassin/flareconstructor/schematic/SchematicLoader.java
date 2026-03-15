package net.kassin.flareconstructor.schematic;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import net.kassin.flareconstructor.FlareConstructorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class SchematicLoader {

    private static final List<String> EXTENSIONS = List.of(".schem", ".schematic");
    private static final List<String> SEARCH_FOLDERS = List.of(
            "FlareConstructor/schematics",
            "flare-constructor/schematics",
            "WorldEdit/schematics",
            "FastAsyncWorldEdit/schematics"
    );

    private SchematicLoader() {}

    public static Clipboard load(FlareConstructorPlugin plugin, String schemName) {
        File file = findFile(plugin, schemName)
                .orElseThrow(() -> new IllegalArgumentException("Schematic '" + schemName + "' não encontrada nas pastas padrão."));

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

    private static Optional<File> findFile(FlareConstructorPlugin plugin, String name) {
        Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();

        return SEARCH_FOLDERS.stream()
                .flatMap(folder -> EXTENSIONS.stream().map(ext -> pluginsDir.resolve(folder).resolve(name + ext)))
                .filter(Files::exists)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .findFirst();
    }
}