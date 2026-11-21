package de.redclient;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class RedClient implements ClientModInitializer {
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        RedConfig.load();

        // Vanilla-Crosshair durch unseren ersetzen
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (ctx, tick) -> {
            if (RedConfig.INSTANCE.customCrosshair) CrosshairRenderer.render(ctx, tick);
            else vanilla.render(ctx, tick);
        });

        // Linke Shift-Taste (im Controls-Menue aenderbar)
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("redclient", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.redclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new RedMenuScreen());
                }
            }
        });

        // /menu und /redmenu
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[]{"menu", "redmenu"}) {
                dispatcher.register(ClientCommandManager.literal(name).executes(c -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.send(() -> mc.setScreen(new RedMenuScreen()));
                    return Command.SINGLE_SUCCESS;
                }));
            }
        });
    }
}
