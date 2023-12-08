package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.yoricya.privat.sota.sotaandradio.php.*;

public final class SotaAndRadio extends JavaPlugin implements Listener {
    private static JSONObject Sotas;
    private static final Logger logger = Logger.getLogger("Sota&Radio Plugin");
    public static Server Server;
    private static final List<Material> passableBlocks = new ArrayList<>();
    private static final ForkJoinPool ThreadPool = new ForkJoinPool();
    private final HashMap<Player, Thread> SotasHandlers = new HashMap<>();
    private boolean isServerOverloaded = false;
    @Override
    public void onEnable() {

        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                logger.log(Level.INFO, "Создаю конфиги...");
                if(!if_dir_exs("Sotas"))
                    mkdir("Sotas");
                if(!if_dir_exs("Sotas/BKP"))
                    mkdir("Sotas/BKP");
                if(!if_file_exs("Sotas/Sotas.json"))
                    file_put_contents("Sotas/Sotas.json", "{}");
                if(!if_file_exs("passable_blocks.txt"))
                    file_put_contents("passable_blocks.txt", Material.AIR.name()+"=");
                logger.log(Level.INFO, "Создал конфиги.");
            }
        });
        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                logger.log(Level.INFO, "Загружаю список Pas-Блоков...");
                try{
                    List<String> psbl = Arrays.asList(file_get_contents("passable_blocks.txt").split("="));
                    psbl.forEach(block -> passableBlocks.add(Material.getMaterial(block)));
                    logger.log(Level.INFO, "Загрузил.");
                }catch(Exception ignore){}
            }
        });

        try {
            getServer().getPluginManager().registerEvents(this, this);
            Server = this.getServer();
            logger.log(Level.INFO, "Загружаю список сот...");
            Sotas = new JSONObject(file_get_contents("Sotas/Sotas.json"));
            logger.log(Level.INFO, "Загрузил список сот.");
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(!getServer().getOnlinePlayers().isEmpty()) for (Player player : getServer().getOnlinePlayers())
                if(SotasHandlers.get(player) == null) onPlayerJoin(new PlayerJoinEvent(player, null));

//        BukkitRunnable br = new BukkitRunnable() {
//            @Override
//            public void run() {
//
//            }
//        };
//        br.runTaskTimerAsynchronously(getPlugin(), 10L, 15L);
    }

    @Override
    public void onDisable() {
        StringBuilder passblocks = new StringBuilder();
        for(int i = 0; i!=passableBlocks.size(); i++){
            passblocks.append(passableBlocks.get(i).name()).append("=");
        }
        file_put_contents("passable_blocks.txt", passblocks.toString());
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, String label, String[] args) {
        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
        try{

            if(command.getName().equals("newPasBlock")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду может выполнять только игрок!");
                    return;
                }

                Player player = (Player) sender;
                Material material = player.getTargetBlock(null, 4).getType(); // Получаем тип материала блока
                if(passableBlocks.contains(material))
                    sender.sendMessage("Блок: "+ material.name()+" - уже добавлен!");
                else {
                    passableBlocks.add(material);
                    sender.sendMessage("Добавлен блок: " + material.name());
                }

            }
            if(command.getName().equals("delPasBlock")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду может выполнять только игрок!");
                    return;
                }

                Player player = (Player) sender;
                Material material = player.getTargetBlock(null, 4).getType(); // Получаем тип материала блока
                passableBlocks.remove(material);
                sender.sendMessage("Блок: "+material.name()+ " - удален.");
            }
            if(command.getName().equals("savePasBlocks")){
                StringBuilder passblocks = new StringBuilder();
                for(int i = 0; i != passableBlocks.size(); i++){
                    passblocks.append(passableBlocks.get(i).name()).append("=");
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
            sot.newSota(((Player) sender) ,args[0], args[1], Float.valueOf(args[2]), args[3]);
            sot.id = Sotas.length();
            Sotas.put(String.valueOf(Sotas.length()), sot.toString());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
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
                    int g = distance(((Player) sender), sotv);
                    if (g <= dist) {
                        dist = g;
                        sot = sotv;
                    }
            }
            if(sot.Name == null){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lБлижайших к вам сот не обнаружено!"));
                return;
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&lБлижайшая к вам сота, ID: &n("+sot.id+")&r&l," +
                    "\n &lИмя: &n("+sot.Name+")&r&l," +
                    "\n &lТип: &n("+sot.Type+")&r&l," +
                    "\n &lМощность: &n("+sot.Wats+")&r&l," +
                    "\n &lКомментарий к соте: &n("+sot.Description+")&r&l," +
                    "\n &lРасстояние: &n("+distance(((Player) sender), sot)+")&r&l."));
           
                return;
        }


        if(command.getName().equals("userParams")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду можно выполнять только от игрока!");
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды:"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&l&nOpTest <1/0>&r&l (Проверять ли соты на соответствие оператора?)" +
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
            if (args[0].equalsIgnoreCase("OpTest")) {
                plrSets.put("optest", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("Radio")) {
                plrSets.put("offradio", args[1].equalsIgnoreCase("0"));
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
            } else if (args[0].equalsIgnoreCase("Mobile")) {
                plrSets.put("offmob", args[1].equalsIgnoreCase("1"));
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lПараметр изменен"));
                return;
            } else if (args[0].equalsIgnoreCase("Operator")) {
                plrSets.put("operator", args[1]);
                file_put_contents("Sotas/" + sender.getName() + ".json", plrSets.toString());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТеперь ваш оператор: &n"+args[1]));
                return;
            }else{
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды:"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&l&nOperatorTest <1/0>&r&l (Проверять ли соты на соответствие оператора)" +
                        "\n &l&nOperator <Имя>&r&l (Установить оператора)" +
                        "\n &l&nRadio <1/0>&r&l (Включить отображение радиостанций)" +
                        "\n &l&nTV <1/0>&r&l (Включить отображение TV)" +
                        "\n &l&nWIFI <1/0>&r&l (Включить отображение WIFI)" +
                        "\n &l&nMOBILE <1/0>&r&l (Включить отображение Мобильных Сетей)"));

            }
        }

        if(command.getName().equalsIgnoreCase("myOperator")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Эту команду можно выполнять только от игрока!");
                return;
            }
            if(args.length == 0){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/myOperator <Имя Оператора>"));
                return;
            }
            ItemStack is = ((Player) sender).getItemInHand();
            ItemMeta im = is.getItemMeta();
            is.setItemMeta(im);
            setTag(is, "op", args[0]);
            ((Player) sender).setItemInHand(is);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lГотово!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lТеперь телефон привязан к оператору: &n"+args[0]));
            return;
        }

        if(command.getName().equalsIgnoreCase("helpSota")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l&n/helpSota&r&a&l - Показать этот список,"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&'," &l&n/myOperator <Имя>&r&l - Установить оператора."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
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
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды: &n/setPhone <Имя Оператора, по умолчанию: 'n'> <Максимально поддерживаемая сеть> <Имя телефона>"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                " &lИмя Оператора можно не указывать, Вместо него вставьте букву &n'n'&r"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                " &a&lВ имени телефона поддерживаются управляющие символы '&n&'&r&a&l которые делают текст цветным! &c&lНО ПРОБЕЛЫ НЕ ДОПУСКАЮТСЯ.&r"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "\n &c&lНБТ Теги телефона наложатся на предмет в вашей руке!"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&lВсе типы сетей:" +
                                "\n &r&n&l1&r&l - &lGSM Только связь и смски. &a&l(Относится к сетям 2G)" +
                                "\n &n&l2&r&l - &lEDGE Связь и слабый интернет. &a&l(Относится к сетям 2G)" +
                                "\n &n&l3&r&l - &l3G Связь и интернет средней скорости. &a&l(Относится к сетям 3G)" +
                                "\n &n&l4&r&l - &l4G Только быстрый интернет. &a&l(Относится к сетям 4G)"));
                        return;
                    }

                    ItemStack is = ((Player) sender).getItemInHand();
                    ItemMeta im = is.getItemMeta();


                    String sim = args[0];
                    if(args[0].equals("n")) sim = "Без ограничений";

                    Objects.requireNonNull(im).setDisplayName(ChatColor.translateAlternateColorCodes('&', args[2]));

                    List<String> phoneLore = new ArrayList<>();
                    phoneLore.add(ChatColor.translateAlternateColorCodes('&', "&7Network-Gen: " +
                            networkGenParse(args[1])));
                    phoneLore.add(ChatColor.translateAlternateColorCodes('&', "&7SIM: "+sim));

                    im.setLore(phoneLore);

                    is.setItemMeta(im);

                    setTag(is, "ph", "1");
                    setTag(is, "op", args[0]);
                    setTag(is, "mn", args[1]);

                    ((Player) sender).setItemInHand(is);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&a&lГотово!"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lУ вас нет прав для использования этой команды!"));
                    return;
                }
            }

            if(command.getName().equalsIgnoreCase("delPhone")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Эту команду можно выполнять только от игрока!");
                    return;
                }
                if (sender.isOp()) {
                    ItemStack is = ((Player) sender).getItemInHand();
                    ItemMeta im = is.getItemMeta();
                    Objects.requireNonNull(im).setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&l(Telephone Deactivated!)"));
                    is.setItemMeta(im);
                    setTag(is, "ph", "0");
                    ((Player) sender).setItemInHand(is);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lГотово!"));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lУ вас нет прав для использования этой команды!"));
                    return;
                }
            }

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
                    if(((Player) sender).getGameMode() == GameMode.CREATIVE){
                        ((Player) sender).setGameMode(GameMode.SURVIVAL);
                    }else if(((Player) sender).getGameMode() == GameMode.SURVIVAL){
                        ((Player) sender).setGameMode(GameMode.CREATIVE);
                    }else{
                        ((Player) sender).setGameMode(GameMode.CREATIVE);
                    }
                        }
                    });
                }else  if(args.length > 1){
                    Player pl = getServer().getPlayer(args[1]);
                    if(pl == null){
                        sender.sendMessage("Игрок не найден!");
                        return;
                    }
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
                        ((Player) sender).setGameMode(GameMode.SURVIVAL);
                    }else
                    if(args[0].equalsIgnoreCase("1")){
                        ((Player) sender).setGameMode(GameMode.CREATIVE);
                    }else
                    if(args[0].equalsIgnoreCase("2")){
                        ((Player) sender).setGameMode(GameMode.ADVENTURE);
                    }else
                    if(args[0].equalsIgnoreCase("3")){
                        ((Player) sender).setGameMode(GameMode.SPECTATOR);
                    }

                }
            });
                }
            }

            if(command.getName().equalsIgnoreCase("infoPhone")) {
                ItemStack is = ((Player) sender).getItemInHand();
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
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a&lИнформация о телефоне:" +
                        "\n &r&lПривязка к оператору: &n"  +v+"&r"+
                        "\n &lПоддержка сетей: &n" +net+"&r"));
            }

        }catch (Exception e){
          e.printStackTrace();
          sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c&lПроизошла ошибка!"));
        }
            }
        });
        return true;
    }

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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final int[] cords = {0, 0, 0};
        final int[] wotkingsteps = {0};

        final JSONObject[] plrSets = {new JSONObject()};
        final ConcurrentHashMap<Integer, BossBar> BSBMap = new ConcurrentHashMap<>();

        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (!if_file_exs("Sotas/" + event.getPlayer().getName() + ".json")) {
                    plrSets[0].put("operator", "none");
                    plrSets[0].put("offmob", false);
                    plrSets[0].put("optest", true);
                    plrSets[0].put("offtv", false);
                    plrSets[0].put("offradio", false);
                    plrSets[0].put("offwifi", false);
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
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
        });

        //0 - Skip, 1 - Ok, 2 - Stop;
        SotaWork script = new SotaWork() {
            @Override
            public int work() {
                if(cords[0] == (int) event.getPlayer().getLocation().getX() &&
                        cords[1] == (int) event.getPlayer().getLocation().getY() &&
                        cords[2] == (int) event.getPlayer().getLocation().getZ()) return 0;

                ThreadPool.execute(new BukkitRunnable() {
                    @Override
                    public void run() {
                        cords[0] = (int) event.getPlayer().getLocation().getX();
                        cords[1] = (int) event.getPlayer().getLocation().getY();
                        cords[2] = (int) event.getPlayer().getLocation().getZ();
                    }
                });

                if(!event.getPlayer().isOnline()) return 2;

                ThreadPool.execute(new Runnable() {
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
                });
                int net = 0;

                double[] netprec = new double[4];
                BossBar bsnet = null;

                if(plrSets[0].getBoolean("optest"))
                    bsnet = getServer().createBossBar("n", BarColor.BLUE, BarStyle.SOLID);

                ItemStack is = event.getPlayer().getItemInHand();
                if(!getTag(is, "ph").equalsIgnoreCase("1")){
                    for (Map.Entry<Integer, BossBar> entry : BSBMap.entrySet()) entry.getValue().removeAll();
                    BSBMap.clear();
                    return 1;
                }

                String currentoperator = plrSets[0].getString("operator");

                String phoneOptag = getTag(is, "op");

                if(!phoneOptag.equals("n")) currentoperator = phoneOptag;

                int maxnet = Integer.parseInt(getTag(is, "mn"));

                ConcurrentHashMap<Integer, BossBar> BossbarsForAdd = new ConcurrentHashMap<>();

                int i = -1;
                while(i < Sotas.length()) {
                    i++;
                    try {
                        Sota sota;
                        try {
                            sota = new Sota(Sotas.getString(String.valueOf(i)));
                            if(sota.Description.equals("OFF")){
                                continue;
                            }
                        } catch (org.json.JSONException ignore){
                            continue;
                        } catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }

                        BarStyle BStyle = BarStyle.SOLID;
                        String st = sota.Type;
                        BarColor bc = BarColor.BLUE;

                        if(st.equalsIgnoreCase("wifi")) {
                            st = "WiFi";
                            bc = BarColor.WHITE;
                            BStyle = BarStyle.SEGMENTED_6;
                        }else if(st.equalsIgnoreCase("wifi5")) {
                            st = "WiFi-5G";
                            bc = BarColor.WHITE;
                            BStyle = BarStyle.SEGMENTED_10;
                        }else if(st.equalsIgnoreCase("wifi6")) {
                            st = "WiFi-6";
                            bc = BarColor.WHITE;
                            BStyle = BarStyle.SEGMENTED_10;
                        }else if(st.equalsIgnoreCase("wifi7")) {
                            st = "WiFi-7";
                            bc = BarColor.WHITE;
                            BStyle = BarStyle.SEGMENTED_10;
                        }else if(st.equalsIgnoreCase("GSM")) {
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(maxnet < 1) continue;

                            bc = BarColor.RED;
                            st = "GSM";

                            if(plrSets[0].getBoolean("optest")) {
                                if(bsnet == null) continue;
                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
                                if(net > 1) continue;

                                double prec = SotaSignalPrecent(event.getPlayer(), sota);
                                if(prec <= 0) continue;

                                bsnet.setColor(BarColor.RED);
                                bsnet.setStyle(BarStyle.SEGMENTED_6);
                                bsnet.setTitle(sota.Name+" (GSM)");

                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                netprec[0] += precforbs;
                                net = 1;
                                continue;
                            }
                        }else if(st.equalsIgnoreCase("EDGE")) {
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(maxnet < 2) continue;

                            bc = BarColor.YELLOW;
                            st = "EDGE";
                            BStyle = BarStyle.SEGMENTED_6;

                            if(plrSets[0].getBoolean("optest")) {
                                if(bsnet == null) continue;
                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
                                if(net > 2) continue;

                                double prec = SotaSignalPrecent(event.getPlayer(), sota);
                                if(prec <= 0) continue;

                                bsnet.setColor(BarColor.YELLOW);
                                bsnet.setStyle(BarStyle.SEGMENTED_6);
                                bsnet.setTitle(sota.Name+" (EDGE)");

                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                netprec[1] += precforbs;
                                net = 2;
                                continue;
                            }
                        }else if(st.equalsIgnoreCase("3G")) {
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(maxnet < 3) continue;

                            bc = BarColor.GREEN;
                            st = "3G";
                            BStyle = BarStyle.SEGMENTED_6;

                            if(plrSets[0].getBoolean("optest")) {
                                if(bsnet == null) continue;
                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
                                if(net > 3) continue;

                                double prec = SotaSignalPrecent(event.getPlayer(), sota);
                                if(prec <= 0) continue;

                                bsnet.setColor(BarColor.GREEN);
                                bsnet.setStyle(BarStyle.SEGMENTED_6);
                                bsnet.setTitle(sota.Name+" (3G)");

                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                netprec[2] += precforbs;
                                net = 3;
                                continue;
                            }
                        }else if(st.equalsIgnoreCase("4G")) {
                            if(plrSets[0].getBoolean("offmob")) continue;
                            if(maxnet < 4) continue;

                            bc = BarColor.GREEN;
                            st = "LTE";
                            BStyle = BarStyle.SEGMENTED_6;

                            if(plrSets[0].getBoolean("optest")) {
                                if(bsnet == null) continue;
                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;

                                double prec = SotaSignalPrecent(event.getPlayer(), sota);
                                if(prec <= 0) continue;

                                bsnet.setColor(BarColor.GREEN);
                                bsnet.setStyle(BarStyle.SEGMENTED_6);
                                bsnet.setTitle(sota.Name+" (LTE)");

                                double precforbs = prec / 100.0;
                                if(precforbs > 1.0) precforbs /= 100.0;
                                netprec[3] += precforbs;
                                net = 4;
                                continue;
                            }
                        }else if(sota.Description.equalsIgnoreCase("TV")){
                            if(plrSets[0].getBoolean("offtv")) continue;
                            st = "TV";
                        }else if(plrSets[0].getBoolean("offradio")) continue;

                        double prec = SotaSignalPrecent(event.getPlayer(), sota);
                        if(prec <= 0) continue;

                        BossBar bossBar = getServer().createBossBar(sota.Name+" ("+st+")", bc, BStyle);
                        double precforbs = prec / 100.0;
                        if(precforbs > 1.0) precforbs /= 100.0;
                        bossBar.setProgress(precforbs);
                        BossbarsForAdd.put(sota.id, bossBar);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                for (Map.Entry<Integer, BossBar> entry : BSBMap.entrySet()) {
                    if(BossbarsForAdd.get(entry.getKey()) == null){
                        entry.getValue().removeAll();
                        BSBMap.remove(entry.getKey());
                    }
                }

                for (Map.Entry<Integer, BossBar> entry : BossbarsForAdd.entrySet()) {
                    BossBar bs = BSBMap.get(entry.getKey());
                    if(bs != null){
                        bs.setProgress(entry.getValue().getProgress());
                        if(!bs.getPlayers().contains(event.getPlayer())) bs.addPlayer(event.getPlayer());
                    }else{
                        BSBMap.put(entry.getKey(), entry.getValue());
                    }
                }

                if(bsnet != null && !plrSets[0].getBoolean("offmob")) {
                    BossBar finalBsnet = bsnet;
                    int finalNet = net;
                    ThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if(finalBsnet.getTitle().equals("n")){
                                    finalBsnet.setTitle(ChatColor.translateAlternateColorCodes('&',"&7[Phone] Нет сети"));
                                    finalBsnet.setColor(BarColor.WHITE);
                                    finalBsnet.setProgress(0);
                                    finalBsnet.setStyle(BarStyle.SEGMENTED_6);
                                }else{
                                    if (netprec[finalNet - 1] > 1.0) netprec[finalNet - 1] = 1.0;
                                    finalBsnet.setProgress(netprec[finalNet - 1]);
                                }

                                BossBar bs = BSBMap.get(-900);
                                if(bs == null){
                                    finalBsnet.addPlayer(event.getPlayer());
                                    BSBMap.put(-900, finalBsnet);
                                }else{
                                    bs.setProgress(finalBsnet.getProgress());
                                    if(!bs.getPlayers().contains(event.getPlayer())) bs.addPlayer(event.getPlayer());
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
                return 0;
            }
        };

        //0 - Skip, 1 - OK, 2 - Stop
        Thread handler = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.interrupted()) {
                    wotkingsteps[0]++;
                    if(wotkingsteps[0] > 2048){
                        onPlayerJoin(event);
                        return;
                    }

                    if (script.work() == 2) return;
                    LockSupport.parkNanos(200);
                }
            }
        });

        handler.start();
        SotasHandlers.put(event.getPlayer(), handler);
    }

    public String networkGenParse(Object dat){
        if(dat instanceof Integer){
            int intDat = (Integer) dat;
            if(intDat == 1) return "GSM";
            if(intDat == 2) return "EDGE";
            if(intDat == 3) return "3G";
            if(intDat == 4) return "4G";
            if(intDat == 5) return "5G";
        }else if(dat instanceof String){
            String strDat = (String) dat;
            if(strDat.equals("1")) return "GSM";
            if(strDat.equals("2")) return "EDGE";
            if(strDat.equals("3")) return "3G";
            if(strDat.equals("4")) return "4G";
            if(strDat.equals("GSM")) return "GSM";
            if(strDat.equalsIgnoreCase("voice")) return "GSM";
            if(strDat.equals("EDGE")) return "EDGE";
            if(strDat.equals("3G")) return "3G";
            if(strDat.equals("4G")) return "4G";
            if(strDat.equals("LTE")) return "4G";
            if(strDat.equals("UMTS")) return "3G";
            if(strDat.equals("HSPA")) return "3G";
            if(strDat.equals("5G")) return "5G";
            if(strDat.equalsIgnoreCase("internet")) return "4G";
        }

        return "Unknown";
    }

    interface SotaWork{
        int work();
    }

    public Plugin getPlugin(){
        return this;
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        SotasHandlers.remove(evt.getPlayer()).interrupt();
    }

    int distance(Player pl, Sota sota){
        Location plocate = pl.getLocation();
        plocate.setY(plocate.getY()+1);
        double distance = plocate.distance(new Location(pl.getWorld(), sota.X, sota.Y, sota.Z));
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

        if(world == null) return 0;

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
                blockCount[0] += (int) hrdns;
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
