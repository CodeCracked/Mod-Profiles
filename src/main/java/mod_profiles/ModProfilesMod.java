package mod_profiles;

import mod_profiles.relauncher.Relauncher;
import mod_profiles.utils.Logging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModProfilesMod implements ClientModInitializer
{
    public static final String MODID = "mod_profiles";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Path MOD_FILE_PATH;

    private boolean ranVersionCheck;
    
    @Override
    public void onInitializeClient()
    {
        ModProfilesKeybinds.register();
        ClientEntityEvents.ENTITY_LOAD.register(this::onWorldLoaded);
        
        try
        {
            File jarFile = new File(ModProfilesMod.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            MOD_FILE_PATH = Path.of(URLDecoder.decode(jarFile.getPath(), StandardCharsets.UTF_8));
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
    
        logLaunchCommand();
    }
    
    private void logLaunchCommand()
    {
        LOGGER.info("#################### LAUNCH ####################");
        List<String> args = new ArrayList<>();
        args.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        Collections.addAll(args, Relauncher.LAUNCH_ARGS);
        
        String classpath = System.getProperty("java.classpath");
        if (classpath != null && classpath.trim().length() > 0)
        {
            args.add("-cp");
            args.add(classpath);
        }
        
        Logging.logArguments(args);
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
