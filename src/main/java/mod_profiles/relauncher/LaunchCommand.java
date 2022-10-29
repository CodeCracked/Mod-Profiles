package mod_profiles.relauncher;

import mod_profiles.ModProfilesMod;
import mod_profiles.utils.Logging;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.*;

public class LaunchCommand
{
    private final List<String> flags;
    private final Map<String, String> variables;
    private String javaLocation;
    private String classpath;
    private String entryPoint;
    
    public LaunchCommand(Collection<String> args)
    {
        flags = new ArrayList<>();
        variables = new HashMap<>();
        
        String variableName = null;
        boolean readingClassPath = false;
        boolean readingEntryPoint = false;
        
        for (String arg : args)
        {
            if (javaLocation == null) javaLocation = arg.trim();
            
            else if (arg.trim().equals("-cp"))
            {
                readingClassPath = true;
            }
            
            else if (readingClassPath)
            {
                classpath = arg.trim();
                readingClassPath = false;
                readingEntryPoint = true;
            }
            
            else if (readingEntryPoint)
            {
                entryPoint = arg.trim();
                readingEntryPoint = false;
            }
            
            else if (variableName != null)
            {
                variables.put(variableName, arg.trim());
                variableName = null;
            }
            
            else if (arg.trim().startsWith("--")) variableName = arg.trim();
            else flags.add(arg.trim());
        }
    }
    
    public String[] buildCommandArray()
    {
        String[] command = new String[4 + flags.size() + variables.size() * 2];
        
        command[0] = javaLocation;
        command[1] = "-cp";
        command[2] = classpath;
        command[3] = entryPoint;
        
        int i = 4;
        for (String flag : flags) command[i++] = flag.trim();
        for (Map.Entry<String, String> variable : variables.entrySet())
        {
            command[i++] = variable.getKey();
            command[i++] = variable.getValue();
        }
        
        return command;
    }
    
    public void run() throws IOException
    {
        String[] builtCommand = buildCommandArray();
        ModProfilesMod.LOGGER.info("Relaunch Command:");
        Logging.logArguments(builtCommand);
        Runtime.getRuntime().exec(builtCommand, null, MinecraftClient.getInstance().runDirectory);
    }
    
    public String getFlag(String search, String defaultValue)
    {
        Optional<String> flag = getFlag(search);
        if (flag.isPresent()) return flag.get();
        else
        {
            flags.add(defaultValue);
            return defaultValue;
        }
    }
    public Optional<String> getFlag(String search)
    {
        for (String flag : flags) if (flag.startsWith(search)) return Optional.of(flag);
        return Optional.empty();
    }
    public void setFlag(String search, String value)
    {
        for (int i = 0; i < flags.size(); i++)
        {
            if (flags.get(i).startsWith(search))
            {
                flags.set(i, value);
                return;
            }
        }
        flags.add(value);
    }
    
    public String getVariable(String name, String defaultValue)
    {
        if (!variables.containsKey(name)) variables.put(name, defaultValue);
        return variables.get(name);
    }
    public Optional<String> getVariable(String name)
    {
        if (variables.containsKey(name)) return Optional.of(variables.get(name));
        else return Optional.empty();
    }
    public void setVariable(String name, String value)
    {
        variables.put(name, value);
    }
}
