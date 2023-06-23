package net.falsesharing.chunky;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkyFolia extends JavaPlugin implements CommandExecutor {

    public record ChunkCoord(int x, int z) {}

    private final ConcurrentLinkedQueue<ChunkCoord> taskQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicLong generatedCount = new AtomicLong(0);
    private World activeWorld;
    private ScheduledTask dispatcherTask;

    @Override
    public void onEnable() {
        if (getCommand("chunky") != null) {
            getCommand("chunky").setExecutor(this);
        }
        getLogger().info("ChunkyFolia initialized with multi-region generation pipeline.");
    }

    @Override
    public void onDisable() {
        stopGeneration();
        getLogger().info("ChunkyFolia shut down.");
    }

    public void startGeneration(World world, int radius) {
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }
        this.activeWorld = world;
        this.taskQueue.clear();
        this.generatedCount.set(0);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                taskQueue.offer(new ChunkCoord(x, z));
            }
        }

        dispatcherTask = Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            if (!isRunning.get() || taskQueue.isEmpty()) {
                stopGeneration();
                return;
            }

            for (int i = 0; i < 64 && !taskQueue.isEmpty(); i++) {
                ChunkCoord coord = taskQueue.poll();
                if (coord == null) break;

                Bukkit.getRegionScheduler().execute(this, activeWorld, coord.x(), coord.z(), () -> {
                    activeWorld.getChunkAtAsync(coord.x(), coord.z(), true).thenAccept(chunk -> {
                        generatedCount.incrementAndGet();
                    });
                });
            }
        }, 50, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void stopGeneration() {
        isRunning.set(false);
        if (dispatcherTask != null) {
            dispatcherTask.cancel();
            dispatcherTask = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
            World w = Bukkit.getWorlds().get(0);
            startGeneration(w, 50);
            sender.sendMessage("Started pregeneration across Folia regions.");
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            stopGeneration();
            sender.sendMessage("Pregeneration stopped.");
            return true;
        }
        sender.sendMessage("Usage: /chunky <start|stop>");
        return true;
    }
}
