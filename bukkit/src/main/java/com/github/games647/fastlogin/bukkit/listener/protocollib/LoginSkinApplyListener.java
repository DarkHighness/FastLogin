package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.reflect.MethodUtils;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class LoginSkinApplyListener implements Listener {

    private static final Class<?> GAME_PROFILE = MinecraftReflection.getGameProfileClass();

    private static final MethodAccessor GET_PROPERTIES = Accessors.getMethodAccessor(GAME_PROFILE, "getProperties");

    private final FastLoginBukkit plugin;

    public LoginSkinApplyListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    //run this on the loginEvent to let skins plugins see the skin like in normal minecraft behaviour
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() != Result.ALLOWED) {
            return;
        }

        Player player = loginEvent.getPlayer();

        if (plugin.getConfig().getBoolean("forwardSkin")) {
            //go through every session, because player.getAddress is null 
            //loginEvent.getAddress is just a InetAddress not InetSocketAddress, so not unique enough
            for (BukkitLoginSession session : plugin.getLoginSessions().values()) {
                if (session.getUsername().equals(player.getName())) {
                    String signature = session.getSkinSignature();
                    String skinData = session.getEncodedSkinData();

                    applySkin(player, skinData, signature);
                    break;
                }
            }
        }
    }

    private void applySkin(Player player, String skinData, String signature) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
        if (skinData != null && signature != null) {
            WrappedSignedProperty skin = WrappedSignedProperty.fromValues("textures", skinData, signature);
            try {
                gameProfile.getProperties().put("textures", skin);
            } catch (ClassCastException castException) {
                Object map = GET_PROPERTIES.invoke(gameProfile.getHandle());
                try {
                    MethodUtils.invokeMethod(map, "put", new Object[]{"textures", skin.getHandle()});
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Error setting premium skin", ex);
                }
            }
        }
    }
}
