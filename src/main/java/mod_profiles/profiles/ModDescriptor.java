package mod_profiles.profiles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod_profiles.ModProfilesMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

public class ModDescriptor
{
    private final String jarFileName;
    private final String name;
    private final String id;
    
    private ModDescriptor(String fileName, String name, String id)
    {
        this.jarFileName = fileName;
        this.name = name;
        this.id = id;
    }
    
    public static ModDescriptor create(File jarFile)
    {
        try (FileSystem fileSystem = FileSystems.newFileSystem(jarFile.toPath()))
        {
            Path sourceFile = fileSystem.getPath("fabric.mod.json");
            File modJsonFile = MinecraftClient.getInstance().runDirectory.toPath().resolve("temp.json").toFile();
            Files.copy(sourceFile, modJsonFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            try(FileReader fileReader = new FileReader(modJsonFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader))
            {
                JsonObject modJson = JsonParser.parseReader(bufferedReader).getAsJsonObject();
                String name = modJson.get("name").getAsString();
                String modID = modJson.get("id").getAsString();
                String version = modJson.get("version").getAsString();
                return new ModDescriptor(jarFile.getName(), name, modID + ":" + version);
            }
            finally
            {
                modJsonFile.delete();
            }
        }
        catch (IOException e)
        {
            ModProfilesMod.LOGGER.error("Failed to create Mod Descriptor for mod jar file: " + jarFile.getPath());
            e.printStackTrace();
            return null;
        }
    }
    
    public String getJarFileName() { return jarFileName; }
    public String getName() { return name; }
    public String getId() { return id; }
    public File getJarFile(Path modsDirectory) { return modsDirectory.resolve(jarFileName).toFile(); }
    
    public static ModDescriptor read(NbtCompound nbt)
    {
        String fileName = nbt.getString("JarFile");
        String name = nbt.getString("Name");
        String id = nbt.getString("ID");
        return new ModDescriptor(fileName, name, id);
    }
    public NbtCompound write()
    {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("JarFile", jarFileName);
        nbt.putString("Name", name);
        nbt.putString("ID", id);
        return nbt;
    }
}
