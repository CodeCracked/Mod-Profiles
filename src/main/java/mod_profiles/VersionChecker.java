package mod_profiles;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VersionChecker
{
    public enum Result
    {
        BEHIND,
        CURRENT,
        AHEAD,
        FAILED
    }
    
    public static String getModVersion()
    {
        ModMetadata metadata = FabricLoader.getInstance().getModContainer(ModProfilesMod.MODID).get().getMetadata();
        return metadata.getVersion().getFriendlyString().split("-")[0];
    }
    public static Result doVersionCheck()
    {
        try
        {
            ModMetadata metadata = FabricLoader.getInstance().getModContainer(ModProfilesMod.MODID).get().getMetadata();
            URL updatesFileURL = new URL(metadata.getCustomValue("updatesFile").getAsString());
            
            String modName = metadata.getName();
            Version modVersion = Version.parse(metadata.getVersion().getFriendlyString().split("-")[0]);
            String minecraftVersion = SharedConstants.getGameVersion().getReleaseTarget();
            String homepage = metadata.getContact().get("homepage").get();

            IniFile updatesFile = IniFile.read(updatesFileURL);
            IniFile.Section latestSection = updatesFile.getSection("Latest");
            IniFile.Section recommendedSection = updatesFile.getSection("Recommended");
            IniFile.Section changelogSection = updatesFile.getSection("Changelog");

            String latestStr = latestSection == null ? null : latestSection.getEntry(minecraftVersion);
            String recommendedStr = recommendedSection == null ? null : latestSection.getEntry(minecraftVersion);

            Version latest = latestStr != null ? Version.parse(latestStr) : null;
            Version recommended = recommendedStr != null ? Version.parse(recommendedStr) : null;

            Version target = recommended != null ? recommended : latest;
            
            // Run Comparison
            if (target == null) return Result.FAILED;
            int versionComparison = modVersion.compareTo(target);
            Result result;
            if (versionComparison < 0) result = Result.BEHIND;
            else if (versionComparison == 0) result = Result.CURRENT;
            else result = Result.AHEAD;
            
            if (result == Result.BEHIND)
            {
                List<MutableText> lines = new ArrayList<>();

                // Out of date message
                lines.add(Text.translatable("version_check.outdated",
                                Text.literal(modName).styled(style -> style.withColor(Formatting.GOLD))).styled(style -> style.withColor(Formatting.GOLD)));
                lines.add(Text.translatable("version_check.currentVersion",
                                Text.literal(modVersion.getFriendlyString()).styled(style -> style.withColor(Formatting.AQUA)),
                                Text.literal(target.getFriendlyString()).styled(style -> style.withColor(Formatting.AQUA))).styled(style -> style.withColor(Formatting.GOLD)));

                // Changelog
                if (changelogSection != null)
                {
                    List<Pair<Version, String>> changelog = new ArrayList<>();
                    changelogSection.forEachEntry((key, value) ->
                    {
                        try
                        {
                            Version version = Version.parse(key);
                            if (modVersion.compareTo(version) < 0) changelog.add(new Pair<>(version, value));
                        } catch (VersionParsingException e)
                        {
                            ModProfilesMod.LOGGER.error("Could not parse changelog entry version key!");
                            e.printStackTrace();
                        }
                    });
                    changelog.sort(Comparator.comparing(Pair::getLeft));
                    if (changelog.size() > 0)
                    {
                        lines.add(Text.literal(""));
                        lines.add(Text.literal("version_check.changelog").styled(style -> style.withColor(Formatting.GOLD)));
                        changelog.forEach(pair ->
                                lines.add(Text.literal("    " + pair.getLeft().getFriendlyString() + ": ").styled(style -> style.withColor(Formatting.AQUA)).append(
                                        Text.literal(pair.getRight()).styled(style -> style.withColor(Formatting.GREEN))
                                )));
                    }
                }

                // Update link
                lines.add(Text.literal(""));
                lines.add(Text.literal("version_check.releasesLink").styled(style -> style.withColor(Formatting.GOLD)).append(Text.literal(" "))
                        .append(Text.literal("version_check.releasesLink.hyperlink").setStyle
                        (
                                Style.EMPTY
                                        .withColor(Formatting.AQUA)
                                        .withUnderline(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, homepage))
                        )));

                for (MutableText line : lines) MinecraftClient.getInstance().player.sendMessage(line, false);
                return Result.BEHIND;
            }
            
            return result;
        }
        catch (IOException | VersionParsingException e)
        {
            if (e instanceof SocketTimeoutException)
            {
                ModProfilesMod.LOGGER.error("Version check timed out!");
            }
            else
            {
                ModProfilesMod.LOGGER.error("Failed to perform version check!");
                e.printStackTrace();
            }

            MinecraftClient.getInstance().player.sendMessage(Text.literal("Failed to perform version check!").styled(style -> style.withColor(Formatting.RED)), false);
            return Result.FAILED;
        }
    }
}
