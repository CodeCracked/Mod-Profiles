package mod_profiles.profiles;

import mod_profiles.ModProfilesMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ModSet
{
    private final String name;
    private final String id;
    private final List<ModDescriptor> mods;
    
    private ModSet(String name, String id, List<ModDescriptor> mods)
    {
        this.name = name;
        this.id = id;
        this.mods = mods;
    }
    public static ModSet create(String name, Path modsDirectory)
    {
        File[] potentialMods = modsDirectory.toFile().listFiles();
        String id = name.toLowerCase().replace(' ', '_');
        List<ModDescriptor> mods = new ArrayList<>();
        for (File potentialMod : potentialMods)
        {
            if (potentialMod.getName().toLowerCase().endsWith(".jar"))
            {
                ModDescriptor mod = ModDescriptor.create(potentialMod);
                mods.add(mod);
            }
        }
        
        return new ModSet(name, id, mods);
    }
    
    public void apply(Path runDirectory)
    {
        System.out.println("######################### APPLYING MOD SET #########################");
        System.out.println("Name: " + name);
        
        Path modsDirectory = runDirectory.resolve("mods");
        Path modsCacheDirectory = runDirectory.resolve("mod_profiles").resolve("mods");
        
        List<File> currentModFiles = new ArrayList<>(List.of(modsDirectory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"))));
        List<String> currentModNames = currentModFiles.stream().map(File::getName).toList();
        
        System.out.println("Current Mods: ");
        currentModNames.forEach(System.out::println);
        
        List<Path> targetModSources = new ArrayList<>();
        List<Path> targetModDestinations = new ArrayList<>();
        List<String> targetModNames = new ArrayList<>();
        for (ModDescriptor mod : mods)
        {
            targetModSources.add(mod.getJarFile(modsCacheDirectory).toPath());
            targetModDestinations.add(mod.getJarFile(modsDirectory).toPath());
            targetModNames.add(mod.getJarFileName());
        }
    
        System.out.println("Target Mods: ");
        targetModNames.forEach(System.out::println);
        
        try
        {
            // Delete mods no longer in use
            for (int i = 0; i < currentModFiles.size(); i++)
            {
                if (!targetModNames.contains(currentModNames.get(i)))
                {
                    System.out.println("Deleting " + currentModFiles.get(i));
                    File file = currentModFiles.get(i);
                    boolean deleted = !file.exists() || file.delete();
                    if (!deleted)
                    {
                        if (i == 0)
                        {
                            int counter = 20;
                            while (!deleted && counter > 0)
                            {
                                counter--;
                                Thread.sleep(250);
                                deleted = file.delete();
                            }
                        }
                        if (!deleted) System.err.println("Failed to delete " + file.getPath());
                    }
                }
            }
    
            // Copy in mods that are now in use
            for (int i = 0; i < targetModSources.size(); i++)
            {
                if (!currentModNames.contains(targetModNames.get(i)))
                {
                    System.out.println("Copying " + targetModSources.get(i) + " to " + targetModDestinations.get(i));
                    Files.copy(targetModSources.get(i), targetModDestinations.get(i), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    //region Serialization
    public static ModSet read(File file)
    {
        try
        {
            return read(NbtIo.read(file));
        }
        catch (IOException e)
        {
            ModProfilesMod.LOGGER.error("Failed to read mod set file: " + file.getPath());
            e.printStackTrace();
            return null;
        }
    }
    public void write(File file)
    {
        try
        {
            NbtIo.write(write(), file);
        }
        catch (IOException e)
        {
            ModProfilesMod.LOGGER.error("Failed to write mod set file: " + file.getPath());
            e.printStackTrace();
        }
    }
    public static ModSet read(NbtCompound nbt)
    {
        String displayName = nbt.getString("Name");
        String id = nbt.getString("ID");
    
        NbtList modsNBT = nbt.getList("Mods", NbtElement.COMPOUND_TYPE);
        List<ModDescriptor> mods = new ArrayList<>(modsNBT.size());
        for (int i = 0; i < modsNBT.size(); i++)
        {
            NbtCompound modNBT = modsNBT.getCompound(i);
            ModDescriptor mod = ModDescriptor.read(modNBT);
            mods.add(mod);
        }
        
        return new ModSet(displayName, id, mods);
    }
    public NbtCompound write()
    {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putString("Name", name);
        nbt.putString("ID", id);
        
        NbtList modsNBT = new NbtList();
        for (ModDescriptor mod : mods) modsNBT.add(mod.write());
        nbt.put("Mods", modsNBT);
        
        return nbt;
    }
    //endregion
}
