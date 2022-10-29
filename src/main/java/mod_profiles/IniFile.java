package mod_profiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IniFile
{
    private Map<String, Section> sections;

    public IniFile()
    {
        this.sections = new HashMap<>();
    }

    public void write(Path path)
    {
        try
        {
            List<String> lines = new ArrayList<>();
            for (Section section : this.sections.values())
            {
                lines.add("[" + section.name + "]");
                section.entries.forEach((key, value) -> lines.add(key + " = " + value));
                lines.add("");
            }
            Files.write(path, lines);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public static IniFile read(Path path) throws IOException
    {
        return read(new IniFileReader(Files.readAllLines(path)));
    }
    public static IniFile read(URL url) throws IOException
    {
        try
        {
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(5000);

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            List<String> lines = new ArrayList<>();

            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) lines.add(inputLine);

            bufferedReader.close();
            return read(new IniFileReader(lines));
        }
        catch (SocketTimeoutException e)
        {
            ModProfilesMod.LOGGER.error("Version check timed out!");
            throw e;
        }
    }
    private static IniFile read(IniFileReader reader)
    {
        IniFile file = new IniFile();

        String line;
        while (reader.hasNextLine())
        {
            line = reader.nextLine();
            if (line.startsWith("[") && line.endsWith("]"))
            {
                String name = line.substring(1, line.length() - 1);
                file.sections.put(name, Section.read(file, name, reader));
            }
        }

        return file;
    }

    public Section getSection(String name) { return this.sections.get(name); }
    public Section newSection(String name)
    {
        Section section = new Section(this, name, new HashMap<>());
        this.sections.put(name, section);
        return section;
    }
    public void forEachSection(Consumer<Section> consumer) { this.sections.forEach((name, section) -> consumer.accept(section)); }


    private static class IniFileReader
    {
        private final List<String> lines;
        private int next;

        public IniFileReader(List<String> lines)
        {
            this.lines = Collections.unmodifiableList(lines);
            this.next = 0;
        }

        public boolean hasNextLine()
        {
            return next < lines.size();
        }
        public String nextLine()
        {
            return lines.get(next++).trim();
        }
        public void back()
        {
            next--;
        }
    }
    public static class Section
    {
        private String name;
        private final IniFile file;
        private final Map<String, String> entries;

        private Section(IniFile file, String name, Map<String, String> entries)
        {
            this.file = file;
            this.name = name;
            this.entries = entries;
        }

        public String getName() { return name; }
        public void setName(String name)
        {
            file.sections.remove(this.name);
            file.sections.put(name, this);
            this.name = name;
        }

        public String getEntry(String key) { return entries.get(key); }
        public void setEntry(String key, String value) { entries.put(key, value); }

        public void forEachEntry(BiConsumer<String, String> consumer) { this.entries.forEach((name, value) -> consumer.accept(name, value)); }

        public static Section read(IniFile file, String name, IniFileReader reader)
        {
            Map<String, String> entries = new HashMap<>();

            String line;
            while (reader.hasNextLine())
            {
                line = reader.nextLine();
                if (line.startsWith("[") && line.endsWith("]"))
                {
                    reader.back();
                    break;
                }
                if (line.startsWith(";") || !line.contains("=")) continue;

                String[] tokens = line.split("=", 2);
                entries.put(tokens[0].trim(), tokens.length > 1 ? tokens[1].trim() : "");
            }

            return new Section(file, name, entries);
        }
    }
}
