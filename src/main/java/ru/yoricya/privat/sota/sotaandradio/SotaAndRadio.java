package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.util.logging.Logger;

import static ru.yoricya.privat.sota.sotaandradio.php.*;

public final class SotaAndRadio extends JavaPlugin implements Listener {
    Logger log =Logger.getLogger("SotaAndRadio");
    public static JSONObject Sotas;
   // public JSONObject allBossbars = new JSONObject();
    public static Server Server;
    @Override
    public void onEnable() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!if_dir_exs("Sotas"))
                    mkdir("Sotas");
                if(!if_dir_exs("Sotas/BKP"))
                    mkdir("Sotas/BKP");
                if(!if_file_exs("Sotas/Sotas.json"))
                    file_put_contents("Sotas/Sotas.json", "{}");
            }
        }).start();

/*        DynmapAPI dynmap = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("Dynmap");

        MarkerAPI markerApi = dynmap.getMarkerAPI(); // Получить доступ к API маркеров
        MarkerSet markerSet = markerApi.getMarkerSet("markerSetId"); // Получить существующий набор маркеров или создать новый
        if (markerSet == null) {
            markerSet = markerApi.createMarkerSet("markerSetId", "Marker Set Name", null, false); // Создать новый набор маркеров
        }

// Создать новый маркер с координатами x, y, z на карте в мире worldName
        MarkerIcon markerIcon = markerApi.getMarkerIcon("iconId"); // Получить иконку маркера
        if (markerIcon == null) {
            markerIcon = markerApi.getMarkerIcon("default"); // Использовать иконку по умолчанию, если не удается найти иконку с указанным идентификатором
        }
        markerSet.createMarker("markerId", "Marker Label", worldName, x, y, z, markerIcon, false); // Создать новый маркер


 */
        try {
            getServer().getPluginManager().registerEvents(this, this);
            Server = this.getServer();
            Sotas = new JSONObject(file_get_contents("Sotas/Sotas.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(getServer().getOnlinePlayers().size() > 0){
            for (Player player : getServer().getOnlinePlayers()){
                onPlayerJoin(new PlayerJoinEvent(player, null));
            }
        }
    }

    @Override
    public void onDisable() {

    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
        try{

        if(command.getName().equals("newSota")){
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду может выполнять только игрок!");
                return;
            }
            if(args.length < 4){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lНе все арги указаны!"));
                sender.sendMessage("/newSota <Имя> <Частота или Тип> <Мощность> <Коментарий>");
                return;
            }
            Sota sot =  new Sota();
            sot.newSota(sender.getServer().getPlayer(sender.getName()) ,args[0], args[1], Float.valueOf(args[2]), args[3]);
            sot.id = Sotas.length();
            Sotas.put(String.valueOf(Sotas.length()), sot.toString());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    "&a&lСота создана! ID cоты:"+sot.id));
           
                return;
        }
        if(command.getName().equals("delSota")){
            if(args.length < 1){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lНе все арги указаны!"));
                sender.sendMessage("/delSota <Имя>");
                return;
            }
            Sota sot = new Sota(Sotas.getString(args[0]));
            sot.Description = "OFF";
            Sotas.put(String.valueOf(args[0]), sot.toString());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&l&eСота удалена!"));
           
                return;
        }
        if(command.getName().equals("nearSota")){
            if(!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lЭту команду можно выполнять только от игрока!"));
                return;
            }
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
            if(sot.Name == null){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lБлижайших к вам сот не обнаружено!"));
                return;
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "" +
                    "&a&lБлижайшая к вам сота, ID: &n("+sot.id+")&r&l," +
                    "\n&lИмя: &n("+sot.Name+")&r&l," +
                    "\n&lТип: &n("+sot.Type+")&r&l," +
                    "\n&lМощность: &n("+sot.Wats+")&r&l," +
                    "\n&lКомментарий к соте: &n("+sot.Description+")&r&l," +
                    "\n&lРасстояние: &n("+distance(getServer().getPlayer(sender.getName()), sot)+")&r&l."));
           
                return;
        }


        if(command.getName().equals("userParams")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду можно выполнять только от игрока!");
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды:"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&l&nOperatorTest <1/0>&r&l (Проверять ли соты на соответствие оператора)" +
                        "\n&l&nOperator <Имя>&r&l (Установить оператора)" +
                        "\n&l&nRadio <1/0>&r&l (Включить отображение радиостанций)" +
                        "\n&l&nTV <1/0>&r&l (Включить отображение TV)" +
                        "\n&l&nWIFI <1/0>&r&l (Включить отображение WIFI)" +
                        "\n&l&nMOBILE <1/0>&r&l (Включить отображение Мобильных Сетей)"));
               
                return;
            }
            JSONObject plrSets = new JSONObject();
            if (!if_file_exs("Sotas/" + sender.getName() + ".json")) {
                plrSets.put("operator", "none");
                plrSets.put("offmob", false);
                plrSets.put("optest", false);
                plrSets.put("offtv", false);
                plrSets.put("offradio", false);
                plrSets.put("offwifi", false);
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
            } else {
                try {
                    plrSets = new JSONObject(file_get_contents("Sotas/" + sender.getName() + ".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (args[0].equalsIgnoreCase("OperatorTest")) {
                plrSets.put("optest", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("Radio")) {
                plrSets.put("offradio", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("TV")) {
                plrSets.put("offtv", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("WIFI")) {
                plrSets.put("offwifi", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("MOBILE")) {
                plrSets.put("offmob", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("Operator")) {
                plrSets.put("operator", args[1]);
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТеперь ваш оператор: &n"+args[0]));
                return;
            }else{
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды:"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&l&nOperatorTest <1/0>&r&l (Проверять ли соты на соответствие оператора)" +
                        "\n&l&nOperator <Имя>&r&l (Установить оператора)" +
                        "\n&l&nRadio <1/0>&r&l (Включить отображение радиостанций)" +
                        "\n&l&nTV <1/0>&r&l (Включить отображение TV)" +
                        "\n&l&nWIFI <1/0>&r&l (Включить отображение WIFI)" +
                        "\n&l&nMOBILE <1/0>&r&l (Включить отображение Мобильных Сетей)"));

            }
        }

        if(command.getName().equalsIgnoreCase("myOperator")) {
            if(args.length == 0){}
            JSONObject plrSets = new JSONObject();
            if (!if_file_exs("Sotas/" + sender.getName() + ".json")) {
                plrSets.put("operator", "none");
                plrSets.put("offmob", false);
                plrSets.put("optest", false);
                plrSets.put("offtv", false);
                plrSets.put("offradio", false);
                plrSets.put("offwifi", false);
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
            } else {
                try {
                    plrSets = new JSONObject(file_get_contents("Sotas/" + sender.getName() + ".json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(args.length == 0){
                plrSets.put("operator", "n");
                return;
            }
            plrSets.put("operator", args[0]);
            file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТеперь ваш оператор: &n"+args[0]));
            return;
        }

        if(command.getName().equalsIgnoreCase("helpSota")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    "&l&n&a/helpSota&r&l&a - Показать этот список,"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&l&n/myOperator <Имя>&r&l - Установить оператора."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    "&l&n/userParam&r&l - Настройка параметров пользователя."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    "&l&n/myOperator <Имя>&r&l - Установить оператора."));
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&l&n/newSota&r&l - Создать новую соту."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&l&n/delSota&r&l - Удалить соту"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&l&n/nearSota&r&l - Найти ближайшую соту."));
            }
        }

        }catch (Exception e){
          e.printStackTrace();
          sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lПроизошла ошибка!"));
        }
            }
        }).start();
        return true;
    }
    public static Material getBlockType(int x, int y, int z){
        return Server.getWorld("World").getBlockAt(x, y, z).getType();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Plugin pl = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int px = (int) e.getPlayer().getLocation().getX();
                int py = (int) e.getPlayer().getLocation().getY();
                int pz = (int) e.getPlayer().getLocation().getZ();
                float p = (int) e.getPlayer().getLocation().getPitch();
                if(pz < -18391) {
                    getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
                        public void run() {
                            getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
                                public void run() {
                                    Location l = new Location(getServer().getWorld("World"), px, py, pz + 2);
                                    l.setPitch(p);
                                    l.setYaw(180);
                                    e.getPlayer().teleport(l);
                                }
                            });
                        }
                    });
                }
                if(pz > 18391) {
                    getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
                        public void run() {
                            getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
                                public void run() {
                                    Location l = new Location(getServer().getWorld("World"), px, py, pz - 2);
                                    //l.setPitch(p);
                                    l.setYaw(180);
                                    e.getPlayer().teleport(l);
                                }
                            });
                        }
                    });
                }
                //9200
            }
        }).start();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final int[] px = {0};
        final int[] py = {0};
        final int[] pz = {0};

        final JSONObject[] plrSets = {new JSONObject()};
        JSONObject Bossbars = new JSONObject();

        new Thread(new Runnable() {
            @Override
            public void run() {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                     //Бекапы
                        new Time(System.nanoTime());
                    }
                }).start();

                if (!if_file_exs("Sotas/" + event.getPlayer().getName() + ".json")) {
                    plrSets[0].put("operator", "none");
                    plrSets[0].put("offmob", false);
                    plrSets[0].put("optest", false);
                    plrSets[0].put("offtv", false);
                    plrSets[0].put("offradio", false);
                    plrSets[0].put("offwifi", false);
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                            "&a&lПривет! &e&lДля помощи по сотам используй &r&l&n/helpSota"));
                    file_put_contents("Sotas/" + event.getPlayer().getName() + ".json", plrSets[0].toString());
                } else {

                    try {
                        plrSets[0] = new JSONObject(file_get_contents("Sotas/" + event.getPlayer().getName() + ".json"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        final int[] ak = {1};
        final boolean[] a = {false};
        new Thread(new BukkitRunnable() {
            @Override
            public void run() {
                if(px[0] == (int)event.getPlayer().getLocation().getX() & py[0] ==(int)event.getPlayer().getLocation().getY() & pz[0] ==(int)event.getPlayer().getLocation().getZ()) {
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        px[0] = (int) event.getPlayer().getLocation().getX();
                        py[0] = (int) event.getPlayer().getLocation().getY();
                        pz[0] = (int) event.getPlayer().getLocation().getZ();
                    }
                }).start();

                    new Thread(new Runnable() {
                    @Override
                    public void run() {
                        file_put_contents("Sotas/Sotas.json", Sotas.toString());

                        try {
                            if(!if_file_exs("Sotas/"+event.getPlayer().getName()+".json")){
                                plrSets[0].put("operator", "none");
                                plrSets[0].put("offmob", false);
                                plrSets[0].put("optest", true);
                                plrSets[0].put("offtv", false);
                                plrSets[0].put("offradio", false);
                                plrSets[0].put("offwifi", false);
                                file_put_contents("Sotas/"+event.getPlayer().getName()+".json", plrSets[0].toString());
                            }else{

                                plrSets[0] = new JSONObject(file_get_contents("Sotas/"+event.getPlayer().getName()+".json"));

                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();

                if(!a[0]){ runTaskTimerAsynchronously(getPlugin(), 1, 10L); a[0] = true;
                    return;
                }

                if(!event.getPlayer().isOnline()){
                    this.cancel();
                    return;
                }
                for(int ib = 0; ib < Bossbars.length(); ib++) {
                    try {
                        BossBar bs = (BossBar) Bossbars.get(String.valueOf(ib));
                        bs.removeAll();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Bossbars.clear();
                int net = 0;
                double[] netprec = new double[4];
                BossBar bsnet = null;
                if(plrSets[0].getBoolean("optest"))  bsnet = getServer().createBossBar("n", BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                for(int i = 0; i < Sotas.length(); i++) {
                    try {
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
                        double prec = SotaSignalPrecent(event.getPlayer(), sota);
                        if(prec <= 0){
                            continue;
                        }
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
                        if(st.equalsIgnoreCase("GSM")) {
                            st = "GSM";
                            bc = BarColor.RED;
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(plrSets[0].getBoolean("optest")) {
                                if (!plrSets[0].getString("operator").equalsIgnoreCase(sota.Name)) continue;
                                if(net > 1) continue;
                                bsnet.setColor(bc);
                                bsnet.setTitle(sota.Name+" ("+st+")");
                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                //bsnet.setProgress(precforbs);
                                netprec[0] += precforbs;
                                net = 1;
                                continue;
                            }
                        }else
                        if(st.equalsIgnoreCase("EDGE")) {
                            st = "EDGE";
                            bc = BarColor.YELLOW;
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(plrSets[0].getBoolean("optest")) {
                                if (!plrSets[0].getString("operator").equalsIgnoreCase(sota.Name)) continue;
                                if(net > 2) continue;
                                bsnet.setColor(bc);
                                bsnet.setTitle(sota.Name+" ("+st+")");
                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                //bsnet.setProgress(precforbs);
                                netprec[1] += precforbs;
                                net = 2;
                                continue;
                            }
                        }else
                        if(st.equalsIgnoreCase("3G")) {
                            st = "3G";
                            bc = BarColor.GREEN;
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(plrSets[0].getBoolean("optest")) {
                                if (!plrSets[0].getString("operator").equalsIgnoreCase(sota.Name)) continue;
                                if(net > 3) continue;
                                bsnet.setColor(bc);
                                bsnet.setTitle(sota.Name+" ("+st+")");
                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                //bsnet.setProgress(precforbs);
                                netprec[2] += precforbs;
                                net = 3;
                                continue;
                            }
                        }else
                        if(st.equalsIgnoreCase("4G")) {
                            st = "LTE";
                            bc = BarColor.GREEN;
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(plrSets[0].getBoolean("optest")) {
                                if (!plrSets[0].getString("operator").equalsIgnoreCase(sota.Name)) continue;
                                if(net > 4) continue;
                                bsnet.setColor(bc);
                                bsnet.setTitle(sota.Name+" ("+st+")");
                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                //bsnet.setProgress(precforbs);
                                netprec[3] += precforbs;
                                net = 4;
                                continue;
                            }
                        }else if(sota.Description.equalsIgnoreCase("TV")){
                            st = "TV";
                            bc = BarColor.BLUE;
                            if(plrSets[0].getBoolean("offtv")) continue;
                        }
                        else{
                            if(plrSets[0].getBoolean("offradio")) continue;
                        }
                        BossBar bossBar = getServer().createBossBar(sota.Name+" ("+st+")", bc, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                        double precforbs = prec / 100.0;
                        if(precforbs > 1.0) precforbs /= 100.0;
                        bossBar.setProgress(precforbs);
                        bossBar.addPlayer(event.getPlayer());
                        Bossbars.put(String.valueOf(Bossbars.length()),bossBar);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                        if(bsnet != null) {
                            if (bsnet.getTitle().equalsIgnoreCase("n")) return;
                            if(netprec[net - 1] > 1.0) netprec[net - 1] = 1.0;
                            bsnet.setProgress(netprec[net - 1]);
                            //double precg = 0;
                           // for(int i = 0; i != 4; i++) {
                            //    if(netprec[i] < precg) precg = netprec[i];
                            //}
                            bsnet.addPlayer(event.getPlayer());
                            Bossbars.put(String.valueOf(Bossbars.length()), bsnet);
                        }

                BukkitRunnable b = this;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(ak[0] >= 20){
                            b.cancel();
                            for(int ib = 0; ib < Bossbars.length(); ib++) {
                                try {
                                    BossBar bs = (BossBar) Bossbars.get(String.valueOf(ib));
                                    bs.removeAll();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if(event.getPlayer().isOnline()) onPlayerJoin(event);
                            return;
                        }
                        ak[0] +=1;
                    }
                }).start();
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
