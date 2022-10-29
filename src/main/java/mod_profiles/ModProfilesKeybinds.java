package mod_profiles;

import mod_profiles.relauncher.Relauncher;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class ModProfilesKeybinds
{
    public static final KeyBinding TEST_KEY = new KeyBinding("mod_profiles.keys.test", GLFW.GLFW_KEY_P, "mod_profiles.keys.category");
    
    public static void register()
    {
        KeyBindingHelper.registerKeyBinding(TEST_KEY);
    
        ClientTickEvents.END_CLIENT_TICK.register(ModProfilesKeybinds::onClientTick);
    }
    
    private static void onClientTick(MinecraftClient client)
    {
        while (TEST_KEY.wasPressed())
        {
            Relauncher.relaunch();
            //Relauncher.relaunch(Screen.hasControlDown());
        }
    }
}
