package cronozx.cullinggames.tasks;

import cronozx.cullinggames.CullingGames;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

public class ChatTask implements Runnable {

    private BukkitTask task;
    private int time = 400;
    //private StartBattleRoyale startTask = new StartBattleRoyale();

    public ChatTask() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(CullingGames.getInstance(), this,0, time);
    }

    @Override
    public void run() {
        Bukkit.getServer().broadcastMessage(ChatColor.RED + "Culling Games >> " + "The Culling Games are starting soon use /queue to join!");
        //Bukkit.getScheduler().runTaskLaterAsynchronously(CullingGames.getInstance(), startTask, 200);
    }
}
