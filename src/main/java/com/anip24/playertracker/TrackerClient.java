package com.anip24.playertracker;

import com.anip24.playertracker.mixin.MinecraftServerAccessor;
import com.sun.jna.platform.win32.OaIdl;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrackerClient implements ClientModInitializer {
    public static File overworldLogFile;
    public static File netherLogFile;
    public static File endLogFile;
    private static ModConfig config;
    private static File basePath;
    private static int prevDay = 0;

    private static String worldName;

    private static MinecraftClient client;

    public static List<PlayerEntity> trackedPlayers = new ArrayList<>();

    public static void RegisterPlayer(PlayerEntity toAdd) {
        if (!trackedPlayers.contains(toAdd)) {
            trackedPlayers.add((toAdd));
            if (config.debugLogging) {
                System.out.println("Player is not already being tracked: " + toAdd);
            }
        }
    }

    public static void CreateFile() {
        try {
            if (client != null) {
                if (client.isInSingleplayer()) {
                    worldName = ((MinecraftServerAccessor) Objects.requireNonNull(client.getServer())).tracker_getSession().getDirectoryName();
                } else {
                    worldName = config.useServerName ? Objects.requireNonNull(client.getCurrentServerEntry()).name : String.valueOf(client.getNetworkHandler().getConnection().getAddress().toString().replace(":", "_"));
                    worldName = worldName.substring(worldName.indexOf("/") + 1);
                }
            }

            System.out.println(worldName);

            String filename = worldName + "-overworld-log-" + LocalDate.now() + ".txt";

            overworldLogFile = new File(basePath, filename);
            System.out.println(overworldLogFile.getAbsolutePath());
            System.out.println(overworldLogFile.exists());

            if (overworldLogFile.createNewFile()) {
                if (config.debugLogging)
                    System.out.println("Overworld log file created: " + filename);
            } else {
                if (config.debugLogging)
                    System.out.println("Overworld log file with name " + filename + " already exists.");
            }

            filename = worldName + "-nether-log-" + LocalDate.now() + ".txt";

            netherLogFile = new File(basePath, filename);
            if (netherLogFile.createNewFile()) {
                if (config.debugLogging)
                    System.out.println("Nether log file created: " + filename);
            } else {
                if (config.debugLogging)
                    System.out.println("Nether log file with name " + filename + " already exists.");
            }

            filename = worldName + "-end-log-" + LocalDate.now() + ".txt";

            endLogFile = new File(basePath, filename);
            if (endLogFile.createNewFile()) {
                if (config.debugLogging)
                    System.out.println("End log file created: " + filename);
            } else {
                if (config.debugLogging)
                    System.out.println("End log file with name " + filename + " already exists.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred in creating a log file:");
            e.printStackTrace();
        }
    }

    public static void LogPositions() {
        for (PlayerEntity player : trackedPlayers) {
            RegistryKey<World> world = player.getWorld().getRegistryKey();

            File targetFile;
            if (world == World.NETHER) {
                targetFile = netherLogFile;
            } else if (world == World.END) {
                targetFile = endLogFile;
            } else {
                targetFile = overworldLogFile;
            }

            try {
                FileWriter myWriter = new FileWriter(targetFile, true);
                myWriter.write(String.format("%s; %s; (%s);\n",
                        LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        player.getName(),
                        player.getBlockPos().toShortString()));
                myWriter.close();
            } catch (IOException e) {
                System.err.println("An error occurred in logging the player position:");
                e.printStackTrace();
            }
        }
    }

    private static boolean worldSwitched() {
        String nWN = "";
        if (client != null) {
            if (client.isInSingleplayer()) {
                nWN = ((MinecraftServerAccessor) Objects.requireNonNull(client.getServer())).tracker_getSession().getDirectoryName();
            } else {
                nWN = config.useServerName ? Objects.requireNonNull(client.getCurrentServerEntry()).name : String.valueOf(client.getNetworkHandler().getConnection().getAddress().toString().replace(":", "_"));
                nWN = nWN.substring(nWN.indexOf("/") + 1);
            }
        }

        if (!nWN.equals(worldName)) {
            worldName = nWN;
            return true;
        } else {
            return false;
        }
    }

    public static void CheckDateChange() {
        if (prevDay != LocalDate.now().getDayOfYear() || worldSwitched()) {
            CreateFile();
            prevDay = LocalDate.now().getDayOfYear();
        }
    }

    public static ModConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        basePath = FabricLoader.getInstance().getGameDir().resolve("playerTracker").toFile();
        prevDay = LocalDate.now().getDayOfYear();

        File containerFolder = new File(basePath.getPath());

        if (!containerFolder.exists()) {
            boolean result = containerFolder.mkdirs();
            if (config.debugLogging)
                System.out.println(result ? "Successfully created" : "Failed to create" + " the log container folder");
        }

        client = MinecraftClient.getInstance();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, _client) -> {
            trackedPlayers.clear();
            TrackerClient.CreateFile();

            if (config.debugLogging)
                System.out.println("Joined new world or server");
        });
    }
}