package mod_profiles.relauncher;

import mod_profiles.ModProfilesMod;
import mod_profiles.VersionChecker;
import mod_profiles.profiles.ModSet;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public final class Relauncher
{
    public static final String LAUNCHER_NAME = "mod-profiles-relauncher";
    public static final String ENTRY_POINT = Relauncher.class.getCanonicalName();
    public static String[] LAUNCH_ARGS;
    
    private static Runnable relaunchTrigger;
    
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        LaunchCommand command = new LaunchCommand(args);
        PrintStream defaultOut = System.out;
        PrintStream defaultErr = System.err;
        
        try (PrintStream out = new PrintStream("C:\\Users\\codec\\Desktop\\relauncher-out.log");
            PrintStream err = new PrintStream("C:\\Users\\codec\\Desktop\\relauncher-err.log"))
        {
            System.setOut(out);
            System.setErr(err);
            
            try
            {
                // Load Mod Set
                Optional<String> runDirectory = command.getVariable("--gameDir");
                Optional<String> modSetPath = command.getVariable("--modSet");
                out.println("Run Directory: " + runDirectory.orElse("NONE"));
                out.println("Mod Set: " + modSetPath.orElse("NONE"));
                
                if (modSetPath.isPresent() && runDirectory.isPresent())
                {
                    ModSet modSet = ModSet.read(new File(modSetPath.get()));
                    if (modSet != null) modSet.apply(Path.of(runDirectory.get()));
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace(err);
            }
            finally
            {
                // Call Relauncher Target Entry Point
                String relauncherTarget = command.getVariable("--relauncherTarget", KnotClient.class.getCanonicalName());
                out.println("Relauncher Target: " + relauncherTarget);
                Class<?> targetClass = Class.forName(relauncherTarget);
                Method entryPoint = targetClass.getMethod("main", String[].class);
                
                out.println("Relauncher Target Valid. Triggering...");
                out.flush();
                err.flush();
                System.setOut(defaultOut);
                System.setErr(defaultErr);
                
                entryPoint.invoke(null, (Object)args);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void relaunch(Path modSet)
    {
        relaunch(command ->
        {
            if (modSet != null && modSet.toFile().exists()) command.setVariable("--modSet", modSet.toString());
        });
    }
    public static void relaunch(Consumer<LaunchCommand> commandModifier)
    {
        // Check if no relaunch is scheduled
        if (relaunchTrigger == null)
        {
            // Create Relaunch Trigger
            String relauncherTarget = Thread.currentThread().getStackTrace()[Thread.currentThread().getStackTrace().length - 1].getClassName();
            relaunchTrigger = () ->
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
        
                // Collect classpath
                String classpathSeparator = System.getProperty("path.separator");
                Set<String> classpath = new HashSet<>();
                Collections.addAll(classpath, ManagementFactory.getRuntimeMXBean().getClassPath().split(classpathSeparator));
                
                // Add minecraft jar to classpath
                Path minecraftJar = MinecraftClient.getInstance().runDirectory.toPath().resolve(FabricLoaderImpl.CACHE_DIR_NAME)
                        .resolve(FabricLoaderImpl.REMAPPED_JARS_DIR_NAME)
                        .resolve("minecraft-" + SharedConstants.getGameVersion().getReleaseTarget() + "-" + FabricLoaderImpl.VERSION)
                        .resolve("client-intermediary.jar");
                if (minecraftJar.toFile().exists()) classpath.add(minecraftJar.toString());
                
                // Add mod to classpath
                classpath.add(ModProfilesMod.MOD_FILE_PATH.toString());
                
                // Add classpath
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
                command.setVariable("--gameDir", MinecraftClient.getInstance().runDirectory.getPath());
                command.setVariable("--relauncherTarget", relauncherTarget);
                if (commandModifier != null) commandModifier.accept(command);
        
                // Relaunch
                try
                {
                    command.run();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            };
    
            // Add Relaunch Shutdown Hook
            Thread relaunchThread = new Thread(relaunchTrigger, "Relaunch Trigger");
            Runtime.getRuntime().addShutdownHook(relaunchThread);
    
            // Close Minecraft
            MinecraftClient.getInstance().scheduleStop();
        }
    }
}
