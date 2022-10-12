package fr.blixow.factionevent.utils.meteorite;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.Messages;
import fr.blixow.factionevent.utils.ScoreBoardAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MeteoriteEvent {

    private Meteorite meteorite;
    private long started;
    private List<Block> blocksArrayList;
    private ArrayList<FallingBlock> fallingBlockArrayList;
    private BukkitTask taskTimer, taskBlocks, taskBlockSet, taskForceBlockSet;
    private boolean allBlocksSet = false;
    private int duration = 1800; // en secondes, par défaut 1800s = 30 mins
    private final FileConfiguration msg;
    private final FileConfiguration fc;
    private ScoreBoardAPI scoreBoardAPI;
    private int maxBlocks = -1;

    public MeteoriteEvent(Meteorite meteorite) {
        FileConfiguration config = FileManager.getConfig();
        this.meteorite = meteorite;
        this.started = new Date().getTime();
        this.blocksArrayList = new ArrayList<>();
        this.msg = FileManager.getMessageFileConfiguration();
        this.fc = FileManager.getMeteoriteDataFC();
        try {
            if (config.contains("meteorite.max_duration")) {
                duration = config.getInt("meteorite.max_duration");
            }
        } catch (Exception ignored) {
        }
        this.start();
    }

    public void start() {
        fallingBlockArrayList = setupFallingBlocks();
        FileConfiguration configuration = FileManager.getConfig();
        int check_time = 1;
        try {
            if (configuration.contains("meteorite.check_time")) {
                check_time = configuration.getInt("meteorite.check_time");
            }
        } catch (Exception ignored) {
        }
        check_time = check_time > 0 ? check_time : 1;
        this.taskTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (checkTimer()) {
                    meteorite.stop();
                    cancel();
                }
            }
        }.runTaskTimer(FactionEvent.getInstance(), 20L, check_time * 20L);

        this.taskBlocks = new BukkitRunnable() {
            @Override
            public void run() {
                if (checkBlocks("")) {
                    meteorite.stop();
                    cancel();
                }
            }
        }.runTaskTimer(FactionEvent.getInstance(), 0L, 100L);

        this.taskBlockSet = new BukkitRunnable() {
            @Override
            public void run() {
                if (allBlocksSet) {
                    cancel();
                }
                ArrayList<FallingBlock> OnGround = new ArrayList<>();
                for (FallingBlock fallingBlock : fallingBlockArrayList) {
                    if (fallingBlock.isOnGround()) {
                        OnGround.add(fallingBlock);
                    }
                }
                for (FallingBlock fallingBlock : OnGround) {
                    fallingBlockArrayList.remove(fallingBlock);
                    if (!blocksArrayList.contains(fallingBlock.getLocation().getBlock())) {
                        blocksArrayList.add(fallingBlock.getLocation().getBlock());
                    }

                }
            }
        }.runTaskTimer(FactionEvent.getInstance(), 0L, 20L);

        this.taskForceBlockSet = new BukkitRunnable() {
            @Override
            public void run() {
                fallingBlockArrayList = new ArrayList<>();
                allBlocksSet = true;
                cancel();
            }
        }.runTaskLater(FactionEvent.getInstance(), 10 * 20L);
        updateScoreboard();
    }

    public void stop() {
        blocksArrayList.forEach(block -> block.setType(Material.AIR));
        taskTimer.cancel();
        taskBlocks.cancel();
        taskBlockSet.cancel();
        taskForceBlockSet.cancel();
        scoreBoardAPI.getObjective().unregister();
        scoreBoardAPI = null;
    }

    public void blockBreak(Block block) {
        while (blocksArrayList.contains(block)) {
            blocksArrayList.remove(block);
        }
        checkBlocks();

    }

    public ArrayList<FallingBlock> setupFallingBlocks() {
        ArrayList<Location> locationArrayList = new ArrayList<>();
        ArrayList<FallingBlock> fallingBlockArrayList = new ArrayList<>();
        World world = meteorite.getLocation().getWorld();
        if (fc.contains(meteorite.getName() + ".block_position")) {
            List<String> stringList = fc.getStringList(meteorite.getName() + ".block_position");
            for (String pos : stringList) {
                try {
                    String[] pos_split = pos.split(",");
                    int x = Integer.parseInt(pos_split[0]), y = Integer.parseInt(pos_split[1]), z = Integer.parseInt(pos_split[2]);
                    Location newLocation = new Location(world, x, y, z);
                    if (!locationArrayList.contains(newLocation)) {
                        locationArrayList.add(newLocation);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        } else {
            locationArrayList.add(meteorite.getLocation());
        }
        ArrayList<Material> mats = new ArrayList<>();
        List<String> stringList = fc.getStringList("block_mats");
        try {
            for (String str : stringList) {
                int percent = 1;
                if (fc.contains("block_percentage." + str)) {
                    percent = fc.getInt("block_percentage." + str);
                }
                Material material = Material.STONE;
                try {
                    material = Material.getMaterial(str);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                for (int i = 0; i < percent; i++) {
                    mats.add(material);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            mats = new ArrayList<>();
        }
        int max = mats.size() - 1;
        int min = 0;
        int nb = 0;
        for (Location loc : locationArrayList) {
            int rand = (int) (Math.random() * (max - min + 1)) + 1;
            if (rand > mats.size() - 1) {
                rand = 0;
            }
            Location spawningLoc = loc.add(0, 5, 0);
            Material random_material = mats.get(rand);
            if (spawningLoc.getChunk().isLoaded() && MeteoriteManager.lowestPlayerDistance(spawningLoc) < 50) {
                FallingBlock fallingBlock = world.spawnFallingBlock(spawningLoc, random_material, (byte) 0);
                fallingBlockArrayList.add(fallingBlock);
            } else {
                int y = world.getHighestBlockYAt(spawningLoc.getBlockX(), spawningLoc.getBlockZ());
                Location highest_loc = new Location(world, spawningLoc.getBlockX(), y, spawningLoc.getBlockZ());
                Block block = highest_loc.getBlock();
                block.setType(random_material);
                blocksArrayList.add(block);
            }
            nb++;

        }
        this.maxBlocks = nb;
        return fallingBlockArrayList;
    }

    public boolean checkTimer() {
        return ((new Date().getTime() - started) / 1000) > duration;
    }

    public void updateScoreboard() {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        long timeRemaining = duration - ((new Date().getTime() - started) / 1000);
        String timeRemainingString = DateManager.getFormattedTime((int) timeRemaining);
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreBoardAPI = new ScoreBoardAPI(board, "Meteorite123", true);
        try {
            String title = msg.getString("meteorite.scoreboard.title");
            List<String> stringList = msg.getStringList("meteorite.scoreboard.lines");
            int size = stringList.size();
            scoreBoardAPI.setDisplayName(title);
            for (String line : stringList) {
                String line2 = new StrManager(line).reBlocks(blocksArrayList.size(), maxBlocks).reTime(timeRemainingString).toString();
                scoreBoardAPI.setLine(size, line2);
                size--;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            scoreBoardAPI.setDisplayName("§8[§cMétéorite§8]");
            scoreBoardAPI.setLine(5, "");
            scoreBoardAPI.setLine(4, "§c» §eBlocks restant");
            scoreBoardAPI.setLine(3, "§7" + blocksArrayList.size() + "§f/§7" + maxBlocks);
            scoreBoardAPI.setLine(2, "");
            scoreBoardAPI.setLine(1, "§c» §eTemps");
            scoreBoardAPI.setLine(0, "§7" + timeRemainingString);
        }
        for (Player player : FactionEvent.getInstance().getEventScoreboardOff().keySet()) {
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (eventManager.isScoreboard()) {
                    player.setScoreboard(scoreBoardAPI.getScoreboard());
                    scoreBoardAPI.getObjective().setDisplaySlot(DisplaySlot.SIDEBAR);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public boolean checkBlocks(String... str) {
        if (blocksArrayList.size() % 5 == 0 || str.length > 0) {
            // update scoreboard and actionbar
            updateScoreboard();

            String message = new StrManager(msg.getString("meteorite.blocks_left_actionbar")).reBlocks(blocksArrayList.size(), maxBlocks).toString();
            message = message == null ? "§c" + blocksArrayList.size() + " blocks §7restant." : message;
            for (Player player : Bukkit.getOnlinePlayers()) {
                Messages.sendActionBar(player, message);
            }
        }
        return allBlocksSet && blocksArrayList.isEmpty();
    }

    public Meteorite getMeteorite() {
        return meteorite;
    }

    public void setMeteorite(Meteorite meteorite) {
        this.meteorite = meteorite;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public List<Block> getBlocksArrayList() {
        return blocksArrayList;
    }

    public ScoreBoardAPI getScoreBoardAPI() {
        return scoreBoardAPI;
    }
}
