package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import static ru.yoricya.zslogin.srv.php.*;

public final class SotaAndRadio extends JavaPlugin implements Listener {
    Logger log =Logger.getLogger("SotaAndRadio");
    public static JSONObject Sotas;
    public static Server Server;
    public JSONObject Bossbars = new JSONObject();
    @Override
    public void onEnable() {
        try {
            getServer().getPluginManager().registerEvents(this, this);
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
        if(command.getName().equals("newSota")){
            if(args.length < 4){
                sender.sendMessage("Не все арги указаны!");
                return false;
            }
            Sota sot =  new Sota();
            sot.newSota(sender.getServer().getPlayer(sender.getName()) ,args[0], args[1], Float.valueOf(args[2]), args[3]);
            sot.id = Sotas.length();
            Sotas.put(String.valueOf(Sotas.length()), sot.toString());
            sender.sendMessage("Сота создана! ID cоты:"+sot.id);
            return true;
        }
        if(command.getName().equals("delSota")){
            if(args.length < 1){
                sender.sendMessage("Не все арги указаны!");
                return false;
            }
            Sota sot = new Sota(Sotas.getString(args[0]));
            sot.Description = "OFF";
            Sotas.put(String.valueOf(args[0]), sot.toString());
            sender.sendMessage("Сота удалена!");
            return true;
        }
        if(command.getName().equals("nearSota")){
            Sota sot = new Sota();
            int dist = 1000;
            for(int i = 0; i < Sotas.length(); i++) {
                    Sota sotv = new Sota(Sotas.getString(String.valueOf(i)));
                    if(sotv.Description.equals("OFF")){
                        continue;
                    }
                    int g = distance(sender.getServer().getPlayer(sender.getName()), sotv);
                    if (g <= dist) {
                        dist = g;
                        sot = sotv;
                    }
            }
            sender.sendMessage("Ближайшая к вам сота, ID: "+sot.id+" - Имя: "+sot.Name+" - Тип: "+sot.Type);
            return true;
        }
        if(command.getName().equals("userParamSota")){
            if(args.length < 2){
            sender.sendMessage("OperatorTest <1/0> (Проверять ли соты на соответствие оператора)" +
                    "\nOperator <Имя> (Установить оператора)" +
                    "\nRadio <1/0> (Выключить отображение радиостанций)" +
                    "\nTV <1/0> (Включить отображение TV)" +
                    "\nWIFI <1/0> (Выключить отображение WIFI)" +
                    "\nMOBILE <1/0> (Выключить отображение Мобильных Сетей)");
            return true;
            }
            JSONObject plrSets = new JSONObject();
            if(!if_file_exs("Sotas/"+sender.getName()+".json")){
                plrSets.put("operator", "none");
                plrSets.put("offmob", false);
                plrSets.put("optest", false);
                plrSets.put("offtv", false);
                plrSets.put("offradio", false);
                plrSets.put("offwifi", false);
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
            }else{
                try {
                    plrSets = new JSONObject(file_get_contents("Sotas/"+sender.getName()+".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(args[0].equalsIgnoreCase("OperatorTest")){
                plrSets.put("optest", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else if(args[0].equalsIgnoreCase("Radio")){
                plrSets.put("offradio", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else if(args[0].equalsIgnoreCase("TV")){
                plrSets.put("offtv", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else if(args[0].equalsIgnoreCase("WIFI")){
                plrSets.put("offwifi", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else if(args[0].equalsIgnoreCase("MOBILE")){
                plrSets.put("offmob", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else if(args[0].equalsIgnoreCase("Operator")){
                plrSets.put("operator", args[1]);
                file_put_contents("Sotas/"+sender.getName()+".json", plrSets.toString());
                return true;
            }else return false;
        }
        return false;
    }
    public static Material getBlockType(int x, int y, int z){
        return Server.getWorld("World").getBlockAt(x, y, z).getType();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Ты вошол на серв значит таймер готов!");
      //  JSONObject bsbars = new JSONObject();
        //bsbars.put("GSM", 0);
        //bsbars.put("EDGE", 0);
        //bsbars.put("3G", 0);
        //bsbars.put("LTE", 0);

        final boolean[] a = {false};
        new Thread(new BukkitRunnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        file_put_contents("Sotas/Sotas.json", Sotas.toString());
                    }
                }).start();
                if(!a[0]){ runTaskTimerAsynchronously(getPlugin(), 1, 10L); a[0] = true;
                   // System.out.println("124");
                    return;
                }
                if(!event.getPlayer().isOnline()){
                    this.cancel();
                    return;
                }
                JSONObject plrSets = new JSONObject();
                try {
                    if(!if_file_exs("Sotas/"+event.getPlayer().getName()+".json")){
                        plrSets.put("operator", "none");
                        plrSets.put("offmob", false);
                        plrSets.put("optest", false);
                        plrSets.put("offtv", false);
                        plrSets.put("offradio", false);
                        plrSets.put("offwifi", false);
                        file_put_contents("Sotas/"+event.getPlayer().getName()+".json", plrSets.toString());
                    }else{
                        plrSets = new JSONObject(file_get_contents("Sotas/"+event.getPlayer().getName()+".json"));
                    }
                    for(int ib = 0; ib < Bossbars.length(); ib++) {
                       // event.getPlayer().sendMessage("Bossbar size:"+Bossbars.length());
                        // System.out.println(Bossbars);
                        try {
                            BossBar bs = (BossBar) Bossbars.get(String.valueOf(ib));
                            bs.removeAll();
                           // bsbars.put("GSM", 0);
                            //bsbars.put("EDGE", 0);
                            //bsbars.put("3G", 0);
                           // bsbars.put("LTE", 0);
                        }catch (Exception e){
                            e.printStackTrace();
                            //System.out.println("Skip:"+ ib);
                        }
                    }
                    Bossbars.clear();
                }catch (Exception e){
                    e.printStackTrace();
                }
                for(int i = 0; i < Sotas.length(); i++) {
                    //event.getPlayer().sendMessage("Сота: "+i+", всего сот:"+Sotas.length());
                    try {

                       // event.getPlayer().sendMessage("1");
                        Sota sota;
                        try {
                            sota = new Sota(Sotas.getString(String.valueOf(i)));
                            if(sota.Description.equals("OFF")){
                                continue;
                            }
                        }catch (Exception e){
                           // e.printStackTrace();
                            continue;
                        }

                       // event.getPlayer().sendMessage("1-1");
                        double prec = SotaSignalPrecent(event.getPlayer(), sota);
                      //  event.getPlayer().sendMessage("1-2");
                        if(prec <= 0){
                          //  event.getPlayer().sendMessage("1-3err: "+prec);
                            continue;
                        }
                       // event.getPlayer().sendMessage("1-4");
                        String st = sota.Type;
                        BarColor bc = BarColor.BLUE;
                        if(st.equalsIgnoreCase("wifi")) {
                            st = "WiFi";
                            bc = BarColor.WHITE;
                        }else
                        if(st.equalsIgnoreCase("wifi5")) {
                            st = "WiFi-5G";
                            bc = BarColor.WHITE;
                        }else
                        if(st.equalsIgnoreCase("wifi6")) {
                            st = "WiFi-6";
                            bc = BarColor.WHITE;
                        }else
                        if(st.equalsIgnoreCase("wifi7")) {
                            st = "WiFi-7";
                            bc = BarColor.WHITE;
                        }else
                        if(st.equalsIgnoreCase("EDGE")) {
                            st = "EDGE";
                            bc = BarColor.YELLOW;
                            if(plrSets.getBoolean("offmob")) continue;
                            if(plrSets.getBoolean("optest"))
                                if (!plrSets.getString("operator").equalsIgnoreCase(sota.Name)) continue;
                          //  int e = bsbars.getInt("EDGE");
                           // if(e > prec){
                            //    continue;
                            //}
                            //bsbars.put("EDGE", prec);
                        }else
                        if(st.equalsIgnoreCase("GSM")) {
                            st = "GSM";
                            bc = BarColor.RED;
                            if(plrSets.getBoolean("offmob")) continue;
                            if(plrSets.getBoolean("optest"))
                                if (!plrSets.getString("operator").equalsIgnoreCase(sota.Name)) continue;
                           // int e = bsbars.getInt("GSM");
                            //if(e > prec){
                              //  continue;
                           // }
                         //   bsbars.put("GSM", prec);
                        }else
                        if(st.equalsIgnoreCase("3G")) {
                            st = "3G";
                            bc = BarColor.GREEN;
                            if(plrSets.getBoolean("offmob")) continue;
                            if(plrSets.getBoolean("optest"))
                                if (!plrSets.getString("operator").equalsIgnoreCase(sota.Name)) continue;
                          //  int e = bsbars.getInt("3G");
                            //if(e > prec){
                              //  continue;
                           // }
                           // bsbars.put("3G", prec);
                        }else
                        if(st.equalsIgnoreCase("4G")) {
                            st = "LTE";
                            bc = BarColor.GREEN;
                            if(plrSets.getBoolean("offmob")) continue;
                            if(plrSets.getBoolean("optest"))
                                if (!plrSets.getString("operator").equalsIgnoreCase(sota.Name)) continue;

                           // int e = bsbars.getInt("LTE");
                            //if(e > prec){
                             //   continue;
                            //}
                            //bsbars.put("LTE", prec);
                        }else if(sota.Description.equalsIgnoreCase("TV")){
                            st = "TV";
                            bc = BarColor.BLUE;
                            if(plrSets.getBoolean("offtv")) continue;
                        }
                        else{
                            if(plrSets.getBoolean("offradio")) continue;
                        }
                        //event.getPlayer().sendMessage("2");
                        BossBar bossBar = Bukkit.createBossBar(sota.Name+" ("+st+")", bc, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                        double precforbs = prec / 100.0;
                        //precforbs += prec % 100.0;
                       // event.getPlayer().sendMessage("2-1: "+precforbs);
                        if(precforbs > 1.0) precforbs /= 100.0;
                        bossBar.setProgress(precforbs);
                        bossBar.addPlayer(event.getPlayer());
                        Bossbars.put(String.valueOf(Bossbars.length()),bossBar);
                       // event.getPlayer().sendMessage("3: "+prec);
                    }catch (Exception e){
                        //e.printStackTrace();
                    }
                }
              //  event.getPlayer().sendMessage("Таймер");
            }
        }).start();

    }
    public Plugin getPlugin(){
        return this;
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {

    }
    void distance(PlayerJoinEvent pl, int x, int y, int z){
        double distance = pl.getPlayer().getLocation().distance(new Location(pl.getPlayer().getWorld(), x,y,z));

    }
    int distance(Player pl, Sota sota){
        int x = sota.X;
        int y = sota.Y;
        int z = sota.Z;
        double distance = pl.getLocation().distance(new Location(pl.getWorld(), x,y,z));

        return Math.toIntExact(Math.round(distance));
    }
    double SotaSignalPrecent(Player pl, Sota sota){
        int precent = 100;
        //int nonprecent = 0;
        //int px = (int) pl.getLocation().getX();
        //int py = (int) pl.getLocation().getY();
        //int pz = (int) pl.getLocation().getZ();
        int maxDist = sota.getMaxDist();
        int dist = distance(pl, sota);
        int res = maxDist - dist;
        //py = sota.getMidHei(py);
       // if(res <= 0){
          //pl.sendMessage("Res:"+res);
        //    return 0;
      //  }
        precent -= (int) ((dist * 6) / sota.Wats);
        /*
        int plrdi = getPlrDr(pl, sota);
        pl.sendMessage("A1:"+py);

        if(plrdi == 1) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px+=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }else
        if(plrdi == 2) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px+=1;
                pz+=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        else
        if(plrdi == 3) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                pz+=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        else
        if(plrdi == 4) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px-=1;
                pz+=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        if(plrdi == 5) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px-=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        if(plrdi == 6) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px-=1;
                pz-=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        if(plrdi == 7) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                pz-=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
        if(plrdi == 8) {
            while (true) {
                if (px == sota.X | py == sota.Y | pz == sota.Z) break;
                px+=1;
                pz-=1;
                Material mat = getBlockType(px, py, pz);
                if(mat != Material.AIR){
                    nonprecent += MaterialsCustom.GetByMate(mat);
                }else{
                    nonprecent += 10;
                }
            }
        }
         */
      //  pl.sendMessage("A2:"+precent);
        if(precent < 0) precent = 0;
        return Double.parseDouble(precent+".0");
    }
    public int getPlrDr(Player player, Sota sota) {

        Location loc1 = player.getLocation();

        double dx = sota.X - loc1.getX();
        double dz = sota.Z - loc1.getZ();

        double angle = Math.atan2(dz, dx);
        double degree = angle * 180 / Math.PI;
        if (degree < 0) {
            degree += 360;
        }

        int direction = 0;

        if (degree >= 337.5 || degree < 22.5) {
            direction = 7;
        } else if (degree >= 22.5 && degree < 67.5) {
            direction = 8;
        } else if (degree >= 67.5 && degree < 112.5) {
            direction = 1;
        } else if (degree >= 112.5 && degree < 157.5) {
            direction = 2;
        } else if (degree >= 157.5 && degree < 202.5) {
            direction = 3;
        } else if (degree >= 202.5 && degree < 247.5) {
            direction = 4;
        } else if (degree >= 247.5 && degree < 292.5) {
            direction = 5;
        } else if (degree >= 292.5 && degree < 337.5) {
            direction = 6;
        }

        return direction;
    }


}
