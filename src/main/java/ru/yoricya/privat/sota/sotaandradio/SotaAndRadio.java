package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
////import org.dynmap.DynmapAPI;
//import org.dynmap.markers.Marker;
//import org.dynmap.markers.MarkerAPI;
//import org.dynmap.markers.MarkerIcon;
//import org.dynmap.markers.MarkerSet;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static ru.yoricya.privat.sota.sotaandradio.php.*;

public final class SotaAndRadio extends JavaPlugin implements Listener {
    Logger log =Logger.getLogger("SotaAndRadio");
    public static JSONObject Sotas;
   // public JSONObject allBossbars = new JSONObject();
    public static Server Server;
    public static List<Material> passableBlocks = new ArrayList<>();
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
                if(!if_file_exs("passable_blocks.txt"))
                    file_put_contents("passable_blocks.txt", Material.AIR.name()+"=");
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    List<String> psbl = Arrays.asList(file_get_contents("passable_blocks.txt").split("="));
                    psbl.forEach(block ->{
                        passableBlocks.add(Material.getMaterial(block));
                    });
                }catch(Exception e){}

            }
        }).start();

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
        StringBuilder passblocks = new StringBuilder();
        for(int i = 0; i!=passableBlocks.size(); i++){
            passblocks.append(passableBlocks.get(i).name()+"=");
        }
        file_put_contents("passable_blocks.txt", passblocks.toString());
    }

    public void addPassBlock(){

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
        try{

            if(command.getName().equals("newPasBlock")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду может выполнять только игрок!");
                    return;
                }

                Player player = sender.getServer().getPlayer(sender.getName());
                int maxDistance = 4; // Максимальная дистанция поиска блока
                Block targetBlock = player.getTargetBlock(null, maxDistance); // Получаем блок, на который смотрит игрок
                if (targetBlock != null) {
                    Material material = targetBlock.getType(); // Получаем тип материала блока
                    if(passableBlocks.contains(material)){
                        sender.sendMessage("Блок: "+ material.name()+" - уже добавлен!");
                    }else {
                        passableBlocks.add(material);
                        sender.sendMessage("Добавлен блок: " + material.name());
                    }
                } else {
                    sender.sendMessage("Блок не найден!");
                }

            }
            if(command.getName().equals("delPasBlock")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду может выполнять только игрок!");
                    return;
                }

                Player player = sender.getServer().getPlayer(sender.getName());
                int maxDistance = 4; // Максимальная дистанция поиска блока
                Block targetBlock = player.getTargetBlock(null, maxDistance); // Получаем блок, на который смотрит игрок
                if (targetBlock != null) {
                    Material material = targetBlock.getType(); // Получаем тип материала блока
                    passableBlocks.remove(material);
                    sender.sendMessage("Блок: "+material.name()+ " - удален.");
                } else {
                    sender.sendMessage("Блок не найден!");
                }

            }
            if(command.getName().equals("savePasBlocks")){
                StringBuilder passblocks = new StringBuilder();
                for(int i = 0; i!=passableBlocks.size(); i++){
                    passblocks.append(passableBlocks.get(i).name()+"=");
                }
                file_put_contents("passable_blocks.txt", passblocks.toString());
            }

        if(command.getName().equals("newSota")){
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду может выполнять только игрок!");
                return;
            }
            if(args.length < 4){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lНе все арги указаны!"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТипы сети:&r" +
                        "\n &r&l&nwifi&r&l - Wi-Fi сеть." +
                        "\n &r&l&ngsm&r&l - GSM (2G) сеть." +
                        "\n &r&l&nedge&r&l - EDGE (2G) сеть."+
                        "\n &r&l&n3g&r&l - 3G (3G) сеть."+
                        "\n &r&l&n4g&r&l - 4G (4G) сеть."+
                        "\n &r&lЛибо укажите частоту: &n'101.2FM'&r"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&l/newSota <Имя> <Частота или Тип> <Мощность в ваттах, пример: '1.0'> <Коментарий, по умолчанию: 'n'>"));
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
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&e&lСота удалена!"));
           
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
                    "\n &lИмя: &n("+sot.Name+")&r&l," +
                    "\n &lТип: &n("+sot.Type+")&r&l," +
                    "\n &lМощность: &n("+sot.Wats+")&r&l," +
                    "\n &lКомментарий к соте: &n("+sot.Description+")&r&l," +
                    "\n &lРасстояние: &n("+distance(getServer().getPlayer(sender.getName()), sot)+")&r&l."));
           
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
                        "\n &l&nOperator <Имя>&r&l (Установить оператора)" +
                        "\n &l&nRadio <1/0>&r&l (Включить отображение радиостанций)" +
                        "\n &l&nTV <1/0>&r&l (Включить отображение TV)" +
                        "\n &l&nWIFI <1/0>&r&l (Включить отображение WIFI)" +
                        "\n &l&nMOBILE <1/0>&r&l (Включить отображение Мобильных Сетей)"));
               
                return;
            }
            JSONObject plrSets = new JSONObject();
            if (!if_file_exs("Sotas/" + sender.getName() + ".json")) {
                plrSets.put("operator", "none");
                plrSets.put("offmob", false);
                plrSets.put("optest", true);
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
                        "\n &l&nOperator <Имя>&r&l (Установить оператора)" +
                        "\n &l&nRadio <1/0>&r&l (Включить отображение радиостанций)" +
                        "\n &l&nTV <1/0>&r&l (Включить отображение TV)" +
                        "\n &l&nWIFI <1/0>&r&l (Включить отображение WIFI)" +
                        "\n &l&nMOBILE <1/0>&r&l (Включить отображение Мобильных Сетей)"));

            }
        }

        if(command.getName().equalsIgnoreCase("myOperator")) {
           /* if(args.length == 0){}
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

            */
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду можно выполнять только от игрока!");
                return;
            }
            if(args.length == 0){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/myOperator <Имя Оператора>"));
                return;
            }
            ItemStack is = ((Player) sender).getPlayer().getItemInHand();
            ItemMeta im = is.getItemMeta();
            is.setItemMeta(im);
            setTag(is, "op", args[0]);
            ((Player) sender).getPlayer().setItemInHand(is);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lГотово!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТеперь телефон привязан к оператору: &n"+args[0]));
            return;
        }

        if(command.getName().equalsIgnoreCase("helpSota")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    "&a&l&n/helpSota&r&a&l - Показать этот список,"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &l&n/myOperator <Имя>&r&l - Установить оператора."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                    " &l&n/userParam&r&l - Настройка параметров пользователя."));
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &6&l&n/newSota&r&l - Создать новую соту."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &6&l&n/delSota&r&l - Удалить соту"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &6&l&n/nearSota&r&l - Найти ближайшую соту."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &9&l&n/setPhone&r&l - Создать новый телефон."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &9&l&n/delPhone&r&l - Деактивировать телефон."));
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &9&l&n/infoPhone&r&l - Информация о телефоне."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &d&l&n/newMark&r&l - Создать маркер на динамической карте."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &d&l&n/delMark&r&l - Удалить маркер на динамической карте."));
        }

            if(command.getName().equalsIgnoreCase("setPhone")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
                    return;
                }
                if (sender.isOp()) {
                    if(args.length < 2){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                                "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/setPhone <Имя Оператора, по умолчанию: 'n'> <Максимально поддерживаемая сеть> <Имя телефона>"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                                " &lИмя Оператора можно не указывать, Вместо него вставьте букву &n'n'&r"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                                " &a&lВ имени телефона поддерживаются управляющие символы '&n&'&r&a&l которые делают текст цветным! &c&lНО ПРОБЕЛЫ НЕ ДОПУСКАЮТСЯ.&r"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                                "\n &c&lНБТ Теги телефона наложатся на предмет в вашей руке!"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                                "&lВсе типы сетей:" +
                                "\n &r&n&l1&r&l - &lGSM Только связь и смски. &a&l(Относится к сетям 2G)" +
                                "\n &n&l2&r&l - &lEDGE Связь и слабый интернет. &a&l(Относится к сетям 2G)" +
                                "\n &n&l3&r&l - &l3G Связь и быстрый интернет. &a&l(Относится к сетям 3G)" +
                                "\n &n&l4&r&l - &l4G Только быстрый интернет. &a&l(Относится к сетям 4G)"));
                        return;
                    }
                    ItemStack is = ((Player) sender).getPlayer().getItemInHand();
                    ItemMeta im = is.getItemMeta();
                    Objects.requireNonNull(im).setDisplayName(ChatColor.translateAlternateColorCodes('&', args[2]));
                    is.setItemMeta(im);
                    setTag(is, "ph", "1");
                    setTag(is, "op", args[0]);
                    setTag(is, "mn", args[1]);
                    ((Player) sender).getPlayer().setItemInHand(is);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lГотово!"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lУ вас нет прав для использования этой команды!"));
                    return;
                }
            }

//            if(command.getName().equalsIgnoreCase("newMark")) {
//                if (!sender.isOp()) {
//                    return;
//                }
//                if(!(sender instanceof Player)) {
//                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
//                    return;
//                }
//                if(args.length < 2){
//                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
//                            "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/newMark <Группа> <Имя> <Комментарий>&r"));
//                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
//                            " &a&lГруппы(Можно так-же писать кастомые группы, &c&lглавное не засирать их!&r&a&l):&r" +
//                            "\n &l&nCity&r&e&l - Группа маркеров для городов.&r" +
//                            "\n &l&nVillage&r&e&l - Группа маркеров для сёл, и деревень.&r"));
//                    return;
//                }
//                int px = (int)((Player) sender).getPlayer().getLocation().getX();
//                int py = (int)((Player) sender).getPlayer().getLocation().getY();
//                int pz = (int)((Player) sender).getPlayer().getLocation().getZ();
//                int id = rand(-999999, 9999999);
//                StringBuilder comment = new StringBuilder();
//                for(int i = 2; i < args.length; i++) {
//                    comment.append(args[i]);
//                    comment.append(" ");
//                }
//                        if(
//                addMarker(String.valueOf(id), args[0], args[1], ((Player) sender).getPlayer().getWorld().getName(),
//                        px, py, pz, comment.toString()))
//                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
//                        "&a&lМаркер создан! ID Маркера: &n"+id+"&r"));
//                        else
//                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&с&lПроизошла ошибка!&r"));
//            }

//            if(command.getName().equalsIgnoreCase("delMark")) {
//                if (!sender.isOp()) {
//                    return;
//                }
//                if(!(sender instanceof Player)) {
//                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
//                    return;
//                }
//                if(args.length < 1){
//                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
//                            "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/delMark <Группа> <ID>&r"));
//                    return;
//                }
//                if(removeMarker(args[1], args[0]))
//                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&e&lМаркер удален!&r"));
//                else sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&с&lПроизошла ошибка!&r"));
//            }

            if(command.getName().equalsIgnoreCase("delPhone")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
                    return;
                }
                if (sender.isOp()) {
                    ItemStack is = ((Player) sender).getPlayer().getItemInHand();
                    ItemMeta im = is.getItemMeta();
                    Objects.requireNonNull(im).setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&l(Telephone Deactivated!)"));
                    is.setItemMeta(im);
                    setTag(is, "ph", "0");
                    ((Player) sender).getPlayer().setItemInHand(is);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lГотово!"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lУ вас нет прав для использования этой команды!"));
                    return;
                }
            }
           /* if(command.getName().equalsIgnoreCase("sethome")) {
                JSONObject plrSets = new JSONObject();
                if (!if_file_exs("Sotas/" + sender.getName() + ".json")) {
                    plrSets.put("operator", "none");
                    plrSets.put("offmob", false);
                    plrSets.put("optest", true);
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
                if(args.length < 1){
                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
                }
            }
*/
            if(command.getName().equalsIgnoreCase("gm")) {
                if (!sender.isOp()) {
                    return;
                }
                if(args.length < 1){
                    if(!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }
                    getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                        public void run() {
                    if(((Player) sender).getPlayer().getGameMode() == GameMode.CREATIVE){
                        ((Player) sender).getPlayer().setGameMode(GameMode.SURVIVAL);
                    }else if(((Player) sender).getPlayer().getGameMode() == GameMode.SURVIVAL){
                        ((Player) sender).getPlayer().setGameMode(GameMode.CREATIVE);
                    }else{
                        ((Player) sender).getPlayer().setGameMode(GameMode.CREATIVE);
                    }
                        }
                    });
                }else  if(args.length > 1){
                    Player pl = getServer().getPlayer(args[1]);
                    getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                        public void run() {
                            if(args[0].equalsIgnoreCase("0")){
                                pl.setGameMode(GameMode.SURVIVAL);
                            }else
                            if(args[0].equalsIgnoreCase("1")){
                                pl.setGameMode(GameMode.CREATIVE);
                            }else
                            if(args[0].equalsIgnoreCase("2")){
                                pl.setGameMode(GameMode.ADVENTURE);
                            }else
                            if(args[0].equalsIgnoreCase("3")){
                                pl.setGameMode(GameMode.SPECTATOR);
                            }
                            else{
                                pl.setGameMode(GameMode.SURVIVAL);
                            }
                            pl.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&lВаш режим игры изменен игроком: &n"+sender.getName()+"&r"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&lРежим игры игрока: &n"+pl.getName()+"&r&l Успешно изменен!"));
                        }
                    });
                }
                else{
                    if(!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }
                    getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                        public void run() {
                    if(args[0].equalsIgnoreCase("0")){
                        ((Player) sender).getPlayer().setGameMode(GameMode.SURVIVAL);
                    }else
                    if(args[0].equalsIgnoreCase("1")){
                        ((Player) sender).getPlayer().setGameMode(GameMode.CREATIVE);
                    }else
                    if(args[0].equalsIgnoreCase("2")){
                        ((Player) sender).getPlayer().setGameMode(GameMode.ADVENTURE);
                    }else
                    if(args[0].equalsIgnoreCase("3")){
                        ((Player) sender).getPlayer().setGameMode(GameMode.SPECTATOR);
                    }

                }
            });
                }
            }

            if(command.getName().equalsIgnoreCase("infoPhone")) {
                ItemStack is = ((Player) sender).getPlayer().getItemInHand();
                String v = "Нет";
                String net = "Нет";
                if(!getTag(is, "op").equalsIgnoreCase("n")){
                    v = getTag(is, "op");
                }
                if(getTag(is, "mn").equalsIgnoreCase("1")){
                    net = "GSM (2G)";
                }else if(getTag(is, "mn").equalsIgnoreCase("2")){
                    net = "EDGE (2G)";
                }else if(getTag(is, "mn").equalsIgnoreCase("3")){
                    net = "3G (3G)";
                }else if(getTag(is, "mn").equalsIgnoreCase("4")){
                    net = "4G (4G)";
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"" +
                        "&a&lИнформация о телефоне:" +
                        "\n &r&lПривязка к оператору: &n"  +v+"&r"+
                        "\n &lПоддержка сетей: &n" +net+"&r"));
            }

        }catch (Exception e){
          e.printStackTrace();
          sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lПроизошла ошибка!"));
        }
            }
        }).start();
        return true;
    }

//    public boolean removeMarker(String id, String group)
//    {
//        DynmapAPI dynmap = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("Dynmap");
//        boolean removed = false;
//        if (dynmap != null && dynmap.markerAPIInitialized())
//        {
//            MarkerAPI markers = dynmap.getMarkerAPI();
//            MarkerSet markerSet = markers.getMarkerSet(group);
//            if (markerSet != null) {
//                Marker marker = markerSet.findMarker(id);
//                if (marker != null) {
//                    removed = true;
//                    marker.deleteMarker();
//                }
//            }
//        }
//        return removed;
//    }

//    public boolean addMarker(String id, String group, String title, String world, int x, int y, int z, String description) {
//        DynmapAPI dynmap = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("Dynmap");
//
//        String ico = "testico.png";
//        String icos = "testico";
//        if(group.equalsIgnoreCase("city")) ico = "cityico.png";
//        if(group.equalsIgnoreCase("city")) icos = "cityico";
//
//        if(group.equalsIgnoreCase("village")) ico = "village.png";
//        if(group.equalsIgnoreCase("village")) icos = "village";
//
//        boolean created = false;
//        if (dynmap != null && dynmap.markerAPIInitialized())
//        {
//            MarkerAPI markers = dynmap.getMarkerAPI();
//
//            MarkerSet markerSet = markers.getMarkerSet(group);
//            if (markerSet == null) {
//                markerSet = markers.createMarkerSet(group, group, null, true);
//            }
//            MarkerIcon wandIcon = markers.getMarkerIcon(icos);
//            if (wandIcon == null) {
//               wandIcon = markers.createMarkerIcon(icos, icos, getPlugin().getResource(ico));
//            }
//            Marker marker = markerSet.findMarker(id);
//            if (marker == null) {
//                created = true;
//                marker = markerSet.createMarker(id, title, world, x, y, z, wandIcon, true);
//            } else {
//                marker.setLocation(world, x, y, z);
//                marker.setLabel(title);
//            }
//            if (description != null) {
//                marker.setDescription(description);
//            }
//        }
//        return created;
//    }

    public ItemStack setTag(ItemStack is, String key, String val){
        NamespacedKey keyp = new NamespacedKey(getPlugin(), key);
        ItemMeta meta= is.getItemMeta();
        if(meta == null) return null;
        meta.getPersistentDataContainer().set(keyp, PersistentDataType.STRING, val);
        is.setItemMeta(meta);
        return is;
    }
    public String getTag(ItemStack is, String key){
        NamespacedKey keyp = new NamespacedKey(getPlugin(), key);
        ItemMeta meta= is.getItemMeta();
        if(meta == null) return "";
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String foundValue = "";
        if(container.has(keyp , PersistentDataType.STRING)) {
            foundValue = container.get(keyp, PersistentDataType.STRING);
        }
        return foundValue;
    }

    public static Material getBlockType(int x, int y, int z){
        return Server.getWorld("World").getBlockAt(x, y, z).getType();
    }



    @EventHandler
    public void onMove(PlayerMoveEvent e) {
//        Plugin pl = this;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int px = (int) e.getPlayer().getLocation().getX();
//                int py = (int) e.getPlayer().getLocation().getY();
//                int pz = (int) e.getPlayer().getLocation().getZ();
//                float p = (int) e.getPlayer().getLocation().getPitch();
//                if(pz < -18391) {
//                    getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
//                        public void run() {
//                            getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
//                                public void run() {
//                                    Location l = new Location(getServer().getWorld("World"), px, py, pz + 2);
//                                    l.setPitch(p);
//                                    l.setYaw(180);
//                                    e.getPlayer().teleport(l);
//                                }
//                            });
//                        }
//                    });
//                }
//                if(pz > 18391) {
//                    getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
//                        public void run() {
//                            getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
//                                public void run() {
//                                    Location l = new Location(getServer().getWorld("World"), px, py, pz - 2);
//                                    //l.setPitch(p);
//                                    l.setYaw(180);
//                                    e.getPlayer().teleport(l);
//                                }
//                            });
//                        }
//                    });
//                }
//                //9200
//            }
//        }).start();
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "&l&n"+e.getPlayer().getName()+"&r&l > "+e.getMessage()+"&r"));
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

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                     //Бекапы
//                       // new Time(System.nanoTime());
//                    }
//                }).start();

                if (!if_file_exs("Sotas/" + event.getPlayer().getName() + ".json")) {
                    plrSets[0].put("operator", "none");
                    plrSets[0].put("offmob", false);
                    plrSets[0].put("optest", true);
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

                if(!a[0]){
                    runTaskTimerAsynchronously(getPlugin(), 1, 15L); a[0] = true;
                    return;
                }

                if(!event.getPlayer().isOnline()){
                    this.cancel();
                    return;
                }

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

                Thread bosbarClearthread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(int ib = 0; ib < Bossbars.length(); ib++) {
                            try {
                                BossBar bs = (BossBar) Bossbars.get(String.valueOf(ib));
                                bs.removeAll();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Bossbars.clear();
                    }
                });
                bosbarClearthread.start();

                int net = 0;
                int maxnet = 5;
                double[] netprec = new double[4];
                BossBar bsnet = null;
                if(plrSets[0].getBoolean("optest")) { bsnet = getServer().createBossBar("n", BarColor.BLUE, BarStyle.SOLID, BarFlag.DARKEN_SKY);
                    ItemStack is = event.getPlayer().getItemInHand();
                    if(!getTag(is, "ph").equalsIgnoreCase("1")) return;
                    plrSets[0].put("operator", getTag(is, "op"));
                    maxnet = Integer.parseInt(getTag(is, "mn"));
                    //net = Integer.parseInt(getTag(is, "mn")) - 1;
                }

                try {
                    bosbarClearthread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for(int i = 0; i < Sotas.length(); i++) {
                    try {
                        Sota sota;
                        try {
                            sota = new Sota(Sotas.getString(String.valueOf(i)));
                            if(sota.Description.equals("OFF")){
                                continue;
                            }
                        }catch (Exception e){
                            continue;
                        }


                        //Тут создается  prec
                        double prec = 0;

                        prec = SotaSignalPrecent(event.getPlayer(), sota);
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
                            if(maxnet < 1) continue;
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
                            if(maxnet < 2) continue;
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
                            if(maxnet < 3) continue;
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
                            if(maxnet < 4) continue;
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
                    bsnet.addPlayer(event.getPlayer());
                    Bossbars.put(String.valueOf(Bossbars.length()), bsnet);
                }

//                if(ak[0] >= 60){
//                    this.cancel();
//                    for(int ib = 0; ib < Bossbars.length(); ib++) {
//                        try {
//                            BossBar bs = (BossBar) Bossbars.get(String.valueOf(ib));
//                            bs.removeAll();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    Bossbars.clear();
//                    if(event.getPlayer().isOnline()) onPlayerJoin(event);
//                    return;
//                }
//                ak[0] +=1;
            }
        }).start();

    }
    public Plugin getPlugin(){
        return this;
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {

    }
    int distance(Player pl, Sota sota){
        double distance = pl.getLocation().distance(new Location(pl.getWorld(), sota.X, sota.Y, sota.Z));
        return Math.toIntExact(Math.round(distance));
    }
    double SotaSignalPrecent(Player pl, Sota sota){
        int precent = 100;
        int dist = distance(pl, sota);
        precent -= (int) ((dist * 4) / sota.Wats);

        if(precent < 0) precent = 0;
        else if(precent > 100) precent = 100;
        else{
            Location plloc = pl.getLocation();
            plloc.setY(plloc.getBlockY());
            Location end = new Location(pl.getWorld(), sota.X, sota.Y, sota.Z);
            precent -= countBlocksOnPath(plloc, end);
        }

        return Double.parseDouble(precent +".0");
    }

    public int countBlocksOnPath(Location start, Location end) {
        World world = start.getWorld();

        final int[] blockCount = {0};
        int dx = Math.abs(end.getBlockX() - start.getBlockX());
        int dy = Math.abs(end.getBlockY() - start.getBlockY());
        int dz = Math.abs(end.getBlockZ() - start.getBlockZ());

        int max = Math.max(Math.max(dx, dy), dz);
        double dxStep = (double) dx / max;
        double dyStep = (double) dy / max;
        double dzStep = (double) dz / max;

        double x = start.getBlockX();
        double y = start.getBlockY();
        double z = start.getBlockZ();
        boolean thb = true;
        for (int i = 0; i <= max; i++) {
            //Для оптимизации
            if(thb){
                thb = false;
                i++;
            }else thb = true;

            Block block = world.getBlockAt((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
            if (block.getType() != org.bukkit.Material.AIR && block.getType() != Material.WATER && block.getType() != Material.LAVA && !passableBlocks.contains(block.getType())) {
                float hrdns = block.getType().getHardness();
                if(hrdns < 1) hrdns = 5;
                if(hrdns > 100) hrdns = 50;
                blockCount[0] += hrdns;
                blockCount[0]++;
            }

            if (x < end.getBlockX()) {
                x += dxStep;
            } else if (x > end.getBlockX()) {
                x -= dxStep;
            }

            if (y < end.getBlockY()) {
                y += dyStep;
            } else if (y > end.getBlockY()) {
                y -= dyStep;
            }

            if (z < end.getBlockZ()) {
                z += dzStep;
            } else if (z > end.getBlockZ()) {
                z -= dzStep;
            }
        }

        return blockCount[0];
    }
}
