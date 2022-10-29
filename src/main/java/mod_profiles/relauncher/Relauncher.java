package mod_profiles.relauncher;

import mod_profiles.ModProfilesMod;
import mod_profiles.VersionChecker;
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Relauncher
{
    public static boolean FROM_RELAUNCH = false;
    public static final String LAUNCHER_NAME = "mod-profiles-relauncher";
    public static final String ENTRY_POINT = Relauncher.class.getCanonicalName();
    
    public static String[] LAUNCH_ARGS;
    
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        FROM_RELAUNCH = true;
        String relauncherTarget = null;
        for (int i = 0; i < args.length - 1; i++) if (args[i].equals("--relauncherTarget")) relauncherTarget = args[i + 1];
        if (relauncherTarget != null)
        {
            Class<?> targetClass = Class.forName(relauncherTarget);
            Method entryPoint = targetClass.getMethod("main", String[].class);
            entryPoint.invoke(null, (Object)args);
        }
    }
    public static void relaunch()
    {
        ModProfilesMod.LOGGER.info("#################### RELAUNCH ####################");
    
        // Rebuild launch command
        List<String> rawCommand = new ArrayList<>();
        
        // Add built-in java
        rawCommand.add(ProcessHandle.current()
                .info()
                .command()
                .orElseThrow());
        
        // Add JVM arguments
        rawCommand.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        
        // Add classpath
        List<String> classpath = new ArrayList<>();
        classpath.add(ManagementFactory.getRuntimeMXBean().getClassPath());
        classpath.add(ModProfilesMod.MOD_FILE_PATH.toString());
        rawCommand.add("-cp");
        rawCommand.add(String.join(System.getProperty("path.separator"), classpath));
    
        // Add entry point
        rawCommand.add(ENTRY_POINT);
        
        // Add Launch arguments
        Collections.addAll(rawCommand, LAUNCH_ARGS);
        
        // Build arguments
        LaunchCommand command = new LaunchCommand(rawCommand);
        command.setFlag("-Dminecraft.launcher.brand", "-Dminecraft.launcher.brand=" + LAUNCHER_NAME);
        command.setFlag("-Dminecraft.launcher.version", "-Dminecraft.launcher.version=" + VersionChecker.getModVersion());
        command.setVariable("--relauncherTarget", KnotClient.class.getCanonicalName());
        
        // Relaunch
        try
        {
            MinecraftClient.getInstance().scheduleStop();
            command.run();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
