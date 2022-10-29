package mod_profiles.utils;

import mod_profiles.ModProfilesMod;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class Logging
{
    private static final Set<String> REDACTED_ARGS = Set.of(
            "--uuid",
            "--accessToken",
            "--clientId",
            "--xuid"
    );
    
    public static void logArguments(String... args)
    {
        logArguments(List.of(args));
    }
    public static void logArguments(Collection<String> args)
    {
        StringBuilder builder = new StringBuilder();
        boolean nextRedacted = false;
        for (String arg : args)
        {
            boolean hasSpaces = arg.contains(" ");
            if (hasSpaces) builder.append('"');
            
            if (nextRedacted)
            {
                builder.append("REDACTED");
                nextRedacted = false;
            }
            else builder.append(arg);
            
            if (hasSpaces) builder.append('"');
            
            if (!arg.startsWith("--"))
            {
                ModProfilesMod.LOGGER.info(builder.toString());
                builder.setLength(0);
            }
            else
            {
                builder.append(' ');
                if (REDACTED_ARGS.contains(arg.trim())) nextRedacted = true;
            }
        }
        if (builder.length() > 0) ModProfilesMod.LOGGER.info(builder.toString());
    }
}
