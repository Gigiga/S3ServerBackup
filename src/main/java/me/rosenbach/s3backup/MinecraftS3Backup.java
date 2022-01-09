package me.rosenbach.s3backup;

import lombok.Getter;
import lombok.Setter;
import me.rosenbach.s3backup.aws.AwsS3Client;
import me.rosenbach.s3backup.commands.BackupCommand;
import me.rosenbach.s3backup.enums.Configuration;
import me.rosenbach.s3backup.tasks.BackupTask;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Timer;

public final class MinecraftS3Backup extends JavaPlugin {

    private static MinecraftS3Backup INSTANCE;

    @Getter
    @Setter
    private boolean running;

    @Getter
    @Setter
    private boolean paused;

    @Getter
    @Setter
    private long lastBackup;

    @Getter
    private AwsS3Client s3;

    private final Timer scheduler;

    public MinecraftS3Backup() {
        INSTANCE = this;
        scheduler = new Timer();
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        if(!configValid()) {
            sendMessage(Bukkit.getConsoleSender(), "Your config is invalid. Please check");
            return;
        }

        registerCommands();

        s3 = new AwsS3Client(
                this.getConfig().getString(Configuration.REGION.getKey()),
                this.getConfig().getString(Configuration.ACCESS_KEY_ID.getKey()),
                this.getConfig().getString(Configuration.ACCESS_KEY_SECRET.getKey()));

        if (intervalConfigured()) {
            long interval = this.getConfig().getLong(Configuration.BACKUP_INTERVAL.getKey()) * 60 * 1000;
            scheduler.schedule(new BackupTask(this, Bukkit.getConsoleSender(), false), interval, interval);

            sendMessage(Bukkit.getConsoleSender(),
                    "Started, running backup task every " +
                            this.getConfig().getLong(Configuration.BACKUP_INTERVAL.getKey()) + " minutes");
        } else {
            sendMessage(Bukkit.getConsoleSender(), "Started, no automatic backup configured");
        }
    }

    @Override
    public void onDisable() {
        scheduler.cancel();
        sendMessage(Bukkit.getConsoleSender(), "Stopped");
    }

    public static MinecraftS3Backup getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MinecraftS3Backup();
        }

        return INSTANCE;
    }

    private boolean configValid() {
        boolean accessKeyFilled = !Objects.requireNonNull(this.getConfig().getString(Configuration.ACCESS_KEY_ID.getKey())).isEmpty();
        boolean accessKeySecretFilled = !Objects.requireNonNull(this.getConfig().getString(Configuration.ACCESS_KEY_SECRET.getKey())).isEmpty();
        boolean regionFilled = !Objects.requireNonNull(this.getConfig().getString(Configuration.REGION.getKey())).isEmpty();
        boolean bucketFilled = !Objects.requireNonNull(this.getConfig().getString(Configuration.BUCKET.getKey())).isEmpty();

        return accessKeyFilled && accessKeySecretFilled && regionFilled && bucketFilled;
    }

    private boolean intervalConfigured() {
        return !Objects.requireNonNull(this.getConfig().getString(Configuration.BACKUP_INTERVAL.getKey())).isEmpty();
    }

    private void registerCommands() {
        Objects.requireNonNull(Bukkit.getPluginCommand("backup")).setExecutor(new BackupCommand());
    }

    public void sendMessage(CommandSender sender, String text) {
        String prefix = ChatColor.DARK_GREEN + "Minecraft S3 Backup: " + ChatColor.GRAY;
        sender.sendMessage(prefix.concat(" ").concat(text));
    }

    public void sendBroadcast(String broadcast) {
        this.getServer().sendMessage(Component.text().content(broadcast).color(TextColor.color(11141120)), MessageType.SYSTEM);
    }
}
