package mod_profiles;

import mod_profiles.profiles.ModSet;
import mod_profiles.relauncher.Relauncher;
import mod_profiles.utils.Logging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.random.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModProfilesMod implements ClientModInitializer
{
    public static final String MODID = "mod_profiles";
    public static final Logger LOGGER = LogManager.getLogger();
    
    public static Path MOD_FILE_PATH;
    public static Path MODS_DIRECTORY;
    public static Path PROFILES_DIRECTORY;
    public static Path MODS_CACHE_DIRECTORY;

    private boolean ranVersionCheck;
    
    @Override
    public void onInitializeClient()
    {
        ModProfilesKeybinds.register();
        ClientEntityEvents.ENTITY_LOAD.register(this::onWorldLoaded);
    
        logLaunchCommand();
    
        try
        {
            Path runDirectory = MinecraftClient.getInstance().runDirectory.toPath();
            File jarFile = new File(ModProfilesMod.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            MOD_FILE_PATH = Path.of(URLDecoder.decode(jarFile.getPath(), StandardCharsets.UTF_8));
            MODS_DIRECTORY = runDirectory.resolve("mods");
            PROFILES_DIRECTORY = runDirectory.resolve("mod_profiles");
            MODS_CACHE_DIRECTORY = PROFILES_DIRECTORY.resolve("mods");
            
            PROFILES_DIRECTORY.toFile().mkdirs();
            MODS_CACHE_DIRECTORY.toFile().mkdirs();
            
            cacheMods();
        }
        catch (IOException | URISyntaxException e)
        {
            e.printStackTrace();
        }
    
        ModSet currentModSet = ModSet.create("Current Mod Set", MinecraftClient.getInstance().runDirectory.toPath().resolve("mods"));
        currentModSet.write(MinecraftClient.getInstance().runDirectory.toPath().resolve("mods.dat").toFile());
    }
    
    private void logLaunchCommand()
    {
        LOGGER.info("#################### LAUNCH ####################");
        List<String> args = new ArrayList<>();
        args.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        Collections.addAll(args, Relauncher.LAUNCH_ARGS);
        
        String classpath = ManagementFactory.getRuntimeMXBean().getClassPath();
        if (classpath != null && classpath.trim().length() > 0)
        {
            args.add("-cp");
            args.add(classpath);
        }
        else
        {
            args.add("-cp");
            args.add("NONE");
        }
        
        Logging.logArguments(args);
    }
    private void cacheMods() throws IOException
    {
        File[] modFiles = MODS_DIRECTORY.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        for (File mod : modFiles)
        {
            Path cachePath = MODS_CACHE_DIRECTORY.resolve(mod.getName());
            if (!cachePath.toFile().exists()) Files.copy(mod.toPath(), cachePath);
        }
    }
    
    private void onWorldLoaded(Entity entity, ClientWorld world)
    {
        if (entity instanceof PlayerEntity)
        {
            if (!ranVersionCheck)
            {
                ranVersionCheck = true;
                VersionChecker.doVersionCheck();
            }
        }
    }
}
