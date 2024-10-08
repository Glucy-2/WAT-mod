package com.anip24.playertracker;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "playertracker")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip(count = 0)
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip(count = 0)
    @Comment("The frequency at which tracked player positions are logged. If set to 20, it is done every once per second. Min 20 ticks.")
    public int frequency = 100;

    @ConfigEntry.Gui.Tooltip(count = 0)
    @Comment("Whether or not to use the name of the server, or the IP address of the connected server, as the name of the world.")
    public boolean useServerName = true;

    @ConfigEntry.Gui.Tooltip(count = 0)
    public boolean debugLogging = false;

    @Override
    public void validatePostLoad() {
        if (frequency < 20) frequency = 20;
    }
}