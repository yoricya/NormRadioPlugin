package ru.yoricya.privat.sota.sotaandradio;

import jdk.jfr.internal.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.JSONObject;

import java.io.IOException;

import static ru.yoricya.zslogin.srv.php.*;

public final class SotaAndRadio extends JavaPlugin {
    public static JSONObject Sotas;
    public static Server Server;
    @Override
    public void onEnable() {
        try {
            Server = this.getServer();
            Sotas = new JSONObject(file_get_contents("Sotas/Sotas.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Plugin startup logic
    }

    @Override
    public void onDisable() {

    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args[0].equals("newSota")){
            if(args.length != 4){
                sender.sendMessage("Не все арги указаны!");
                return false;
            }
            Sota sot =  new Sota();
            sot.newSota(sender.getServer().getPlayer(sender.getName()) ,args[1], args[2], Float.valueOf(args[3]), args[4]);
            Sotas.length();
            Sotas.put(String.valueOf(Sotas.length() + 1), sot);

        }
        return false;
    }
    public static Material getBlockType(int x, int y, int z){
        return Server.getWorld("World").getBlockAt(x, y, z).getType();
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
                BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                int a = scheduler.scheduleAsyncRepeatingTask(this, new Runnable(){
                    @Override
                    public void run(){
                        if(!event.getPlayer().isOnline()){
                           // scheduler.cancelTask(a);
                        }
                    }
                }, 100, 100);

    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {

    }
    void distance(PlayerJoinEvent pl, int x, int y, int z){
        double distance = pl.getPlayer().getLocation().distance(new Location(pl.getPlayer().getWorld(), x,y,z));

    }

}
