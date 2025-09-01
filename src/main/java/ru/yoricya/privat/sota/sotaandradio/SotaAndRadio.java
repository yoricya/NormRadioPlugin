package ru.yoricya.privat.sota.sotaandradio;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.checkerframework.checker.nullness.qual.NonNull;

import ru.yoricya.privat.sota.sotaandradio.v2.*;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SotaAndRadio extends JavaPlugin implements Listener {

    public static StationDb stationDb;

    public static PlayerDb playerDb;

    static final Logger logger = Logger.getLogger("Sota&Radio Plugin");

    static final ForkJoinPool ThreadPool = new ForkJoinPool();

    @Override
    public void onEnable() {
        logger.log(Level.INFO, "Initializing plugin...");
        long startTime = System.currentTimeMillis();

        stationsDbLoader();
        playersDbLoader();

        new File("plugins"+File.separator+"SotaAndRadio"+File.separator+"stationBackups").mkdirs();

        if (!getServer().getOnlinePlayers().isEmpty()) for (Player player : getServer().getOnlinePlayers()){
            var playerData = playerDb.loadPlayer(player);
            if (!playerData.isInit()){
                onPlayerJoin(new PlayerJoinEvent(player, null));
            }
        }

        getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), this::saveAllData, 1000L, 1000L);
        getServer().getPluginManager().registerEvents(this, this);

        logger.log(Level.INFO, "Plugin initializing done with "+(System.currentTimeMillis() - startTime)+" ms.");
    }

    void stationsDbLoader() {
        logger.log(Level.INFO, "Loading stationsDb.");
        long startTime = System.currentTimeMillis();

        try {
            stationDb = new StationDb(new File("plugins"+File.separator+"SotaAndRadio"+File.separator+"stationsDb.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.log(Level.INFO, "Loading stationsDb done with "+(System.currentTimeMillis() - startTime)+"ms.");
        }
    }

    void playersDbLoader() {
        logger.log(Level.INFO, "Loading playersDb.");
        long startTime = System.currentTimeMillis();

        try {
            playerDb = new PlayerDb(new File("plugins"+File.separator+"SotaAndRadio"+File.separator+"playersDb.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.log(Level.INFO, "Loading playersDb done with "+(System.currentTimeMillis() - startTime)+"ms.");
        }
    }

    @Override
    public void onDisable() {
        logger.log(Level.INFO, "Saving data...");
        long startTime = System.currentTimeMillis();

        saveAllData();

        logger.log(Level.INFO, "Data saved with "+(System.currentTimeMillis() - startTime)+"ms.");
    }

    void saveAllData() {
        stationDb.saveDb();
        playerDb.saveDb();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command,  @NonNull String label, @NonNull String[] args) {
        ThreadPool.execute(() -> {
            if (!command.testPermission(sender)) {
                sender.sendMessage("Нет прав на использование данной команды!");
                return;
            }

            try {

                // Создать соту - /newSota <Тип>
                if (command.getName().equalsIgnoreCase("newSota")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду может выполнять только игрок!");
                        return;
                    }

                    // Station Type
                    String stationType = "";

                    if (args.length >= 1) {
                        stationType = args[0].toLowerCase();
                    }

                    // Random Id for station
                    Long id_of_station = null;

                    // FM Radio - /newSota radio <Freq> <Power> <Name>
                    if (stationType.equals("radio")) {

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&c&lНе все арги указаны!" +
                                            "\n&e&l/newSota radio <Частота> <Мощность> <Имя> "));
                            return;
                        }

                        // Генерируем ID
                        id_of_station = stationDb.randomId();

                        double freq;
                        double power;

                        // Парсим значения
                        try{
                            freq = Double.parseDouble(args[1]);
                            power = Double.parseDouble(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lЧастота и мощность должны быть числом!"));
                            return;
                        }

                        // Создаем станцию
                        FMStation station = new FMStation();
                        station.stationLocation = ((Player) sender).getLocation().clone();
                        station.id = id_of_station;
                        station.power = power;
                        station.frequency = freq;
                        station.name = args[3];

                        // Добавляем станцию
                        stationDb.addStation(station);
                    } else if (stationType.equals("tv")) {
                        // Tv Radio - /newSota tv <Freq> <Power> <Name>

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&c&lНе все арги указаны!" +
                                            "\n&e&l/newSota tv <Частота> <Мощность> <Имя> "));
                            return;
                        }

                        // Генерируем ID
                        id_of_station = stationDb.randomId();

                        double freq;
                        double power;

                        // Парсим значения
                        try{
                            freq = Double.parseDouble(args[1]);
                            power = Double.parseDouble(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lЧастота и мощность должны быть числом!"));
                            return;
                        }

                        // Создаем станцию
                        TVStation station = new TVStation();
                        station.stationLocation = ((Player) sender).getLocation().clone();
                        station.id = id_of_station;
                        station.power = power;
                        station.frequency = freq;
                        station.name = args[3];

                        // Добавляем станцию
                        stationDb.addStation(station);
                    } else if (stationType.equals("mobilebs")) {
                        // Mobile Base Station - /newSota mobilebs <Sota name> <Power> <Options>

                        if (args.length < 4) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&c&lНе все арги указаны!" +
                                            "\n&e&l/newSota mobilebs <Имя соты> <Мощность> <Опции соты, формата 'key1:val1,val2;key2:val3,val4'> "));
                            return;
                        }

                        // Генерируем ID
                        id_of_station = stationDb.randomId();

                        double power;

                        // Парсим значения
                        try{
                            power = Double.parseDouble(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lЧастота и мощность должны быть числом!"));
                            return;
                        }

                        // Создаем станцию
                        MobileBaseStation mobileBaseSattion = new MobileBaseStation();

                        mobileBaseSattion.stationLocation = ((Player) sender).getLocation().clone();
                        mobileBaseSattion.id = id_of_station;
                        mobileBaseSattion.power = power;
                        mobileBaseSattion.name = args[1];

                        // Парсим опции
                        for (CellularNetworkConfig.Config config: CellularNetworkConfig.ParseParams(args[3])){
                            switch (config){
                                case CellularNetworkConfig.Mcc mccConfig -> {
                                    mobileBaseSattion.supportMnc = mccConfig.allowMccList.stream()
                                            .mapToInt(Integer::intValue)
                                            .boxed() // int -> Integer
                                            .collect(Collectors.toSet());
                                }

                                case CellularNetworkConfig.Mnc mncConfig -> {
                                    mobileBaseSattion.supportMnc = mncConfig.allowMncList.stream()
                                            .mapToInt(Integer::intValue)
                                            .boxed() // int -> Integer
                                            .collect(Collectors.toSet());
                                }

                                case CellularNetworkConfig.RoamingPolicyAllow ignored -> {
                                    mobileBaseSattion.allowIncomingRoaming = true;
                                }

                                case CellularNetworkConfig.NetworkGenerations generations -> {
                                    mobileBaseSattion.generation = generations.supportedGenerations.getFirst();
                                }

                                default -> throw new IllegalStateException("Unexpected value: " + config);
                            }
                        }

                        // Добавляем станцию
                        stationDb.addStation(mobileBaseSattion);
                    }

                    // Если произошла ошибка
                    if (id_of_station == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lНе все арги указаны!"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lТипы станций:&r" +
                                "\n &r&l&nwifi&r&l - Wi-Fi сеть." +
                                "\n &r&l&nmobilebs&r&l - Мобильная Базовая Станция." +
                                "\n &r&l&nradio&r&l - FM Радио." +
                                "\n &r&l&ntv&r&l - TV Станция."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&e&l/newSota <Тип> ..."));
                        return;
                    }

                    // Если все окей
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&lСота создана! ID cоты:" + id_of_station));

                    // Ну и сейвим, да
                    saveAllData();

                    //Сделай бекапы аааааа!
//                        file_put_contents("Sotas"+File.separator+"Sotas.json", Sotas.toString());
//                        ThreadPool.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                synchronized (SotasHandlers) {
//                                    String filename = System.currentTimeMillis() + "bkp.json";
//                                    file_put_contents("Sotas" + File.separator + "BKP" + File.separator + filename, Sotas.toString());
//                                }
//                            }
//                        });

                    return;
                }

                // Удалить соту
                if (command.getName().equalsIgnoreCase("delSota")) {
                    if (args.length < 1 || !isNum(args[0])) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lНе все арги указаны!"));
                        sender.sendMessage("/delSota <ID>");
                        return;
                    }

                    var id = Long.parseLong(args[0]);
                    if (stationDb.removeStation(id))
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&lСота удалена!"));
                    else
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&lСота не существует!"));

                    return;
                }

                // Ближайшие соты
                if (command.getName().equalsIgnoreCase("nearsota")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lЭту команду можно выполнять только от игрока!"));
                        return;
                    }

                    var player_location = ((Player) sender).getLocation().clone();

                    List<StationDb.NearStationResult> listOfStations = stationDb.nearStations(player_location, station -> true);

                    if (listOfStations.isEmpty()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lБлижайших к вам сот не обнаружено."));
                        return;
                    }

                    if (listOfStations.size() > 4)
                        listOfStations.subList(0, listOfStations.size() - 4).clear();

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "\n\n&a&lБлижайшие к вам соты:"));

                    for (StationDb.NearStationResult stationResult : listOfStations) {
                        ComponentBuilder cb = new ComponentBuilder();
                        cb.event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(stationResult.station.id)));

                        cb.append(ChatColor.translateAlternateColorCodes('&',"----------------------\n"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"&lID: "+stationResult.station.id));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &lName: "+stationResult.station.getName()));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &lSignal: "+stationResult.signalPrecent +"%"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &lDistance: "+Math.round(stationResult.station.stationLocation.distance(player_location)) +" blocks"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &l&nClick to copy ID."));

                        sender.spigot().sendMessage(cb.build());
                    }

                    return;
                }

                // Управление параметрами пользователя - /userParams <parameter> <value>
                if (command.getName().equalsIgnoreCase("userParams")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    String param = "";
                    String value = null;

                    if (args.length > 0) {
                        param = args[0];
                    }

                    if (args.length > 1) {
                        value = args[1];
                    }

                    PlayerData playerData = playerDb.loadPlayer(((Player) sender));

                    // Параметр отключающий за обнаружение радио вышек
                    if (param.equalsIgnoreCase("off_radio")) {
                        if (value != null) {
                            playerData.paramOffRadio.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&a&lТекущее значение: " + playerData.paramOffRadio));
                        }
                    } else

                    // Параметр отвечающий за обнаружение ТВ вышек
                    if (param.equalsIgnoreCase("off_tv")) {
                        if (value != null) {
                            playerData.paramOffTv.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&a&lТекущее значение: "+playerData.paramOffTv));
                        }
                    } else

                    // Параметр отвечающий за обнаружение сотовых сетей
                    if (param.equalsIgnoreCase("off_mobile")) {
                        if (value != null) {
                            playerData.paramOffMobile.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&a&lТекущее значение: "+playerData.paramOffMobile));
                        }
                    } else

                    // Параметр отвечающий за обнаружение Wi-Fi сетей
                    if (param.equalsIgnoreCase("off_wifi")) {
                        if (value != null) {
                            playerData.paramOffWifi.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&a&lТекущее значение: "+playerData.paramOffWifi));
                        }
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                """
                                      
                                      &e&lЧто-то не то! Проверьте правильно ли вы вводите команды:
                                        &r&l&noff_radio <true/false>&r&l (Отображение радиостанций)
                                        &l&noff_tv <true/false>&r&l (Отображение TV станций)
                                        &l&noff_wifi <true/false>&r&l (Отображение Wi-Fi сетей)
                                        &l&noff_mobile <true/false>&r&l (Отображение мобильных сетей)"""));
                    }

                    return;
                }

                //cmd
                if (command.getName().equalsIgnoreCase("helpSota")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&l&n/helpSota&r&a&l - Показать этот список,"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &l&n/myOperator <Имя>&r&l - Установить оператора."));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            " &l&n/userParam&r&l - Настройка параметров пользователя."));
                    if (sender.isOp()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &6&l&n/newSota&r&l - Создать новую соту."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &6&l&n/delSota&r&l - Удалить соту"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &6&l&n/nearSota&r&l - Найти ближайшую соту."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &9&l&n/setPhone&r&l - Создать новый телефон."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &9&l&n/delPhone&r&l - Деактивировать телефон."));
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &9&l&n/infoPhone&r&l - Информация о телефоне."));
                    //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &d&l&n/newMark&r&l - Создать маркер на динамической карте."));
                    //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &d&l&n/delMark&r&l - Удалить маркер на динамической карте."));
                    return;
                }

                // Создает "Телефон" /setPhone <Phone name> <Other params>
                // Other params: net:gsm,gprs,edge,cdma,hspa,hspa+,lte;mcc:250,251;mnc:001,002;allowRoaming:true
                if (command.getName().equalsIgnoreCase("setPhone")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lУ вас нет прав для использования этой команды!"));
                        return;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&e&lЧто-то не то! Проверьте правильно ли вы вводите команды:\n  &n/setPhone <Имя телефона> <Параметры формата 'key1:val1,val2;key2:val3,val4'>"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "  &a&lВ имени телефона поддерживаются управляющие символы '&n&'&r&a&l которые делают текст цветным! &c&lНО ПРОБЕЛЫ НЕ ДОПУСКАЮТСЯ.&r"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "  &c&lНБТ Теги телефона наложатся на предмет в вашей руке!"));
//                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
//                                        "&lВсе типы сетей:" +
//                                                "\n &r&n&l1&r&l - &lGSM Только связь и смски. &a&l(Относится к сетям 2G)" +
//                                                "\n &n&l2&r&l - &lEDGE Связь и слабый интернет. &a&l(Относится к сетям 2G)" +
//                                                "\n &n&l3&r&l - &l3G Связь и интернет средней скорости. &a&l(Относится к сетям 3G)" +
//                                                "\n &n&l4&r&l - &l4G Только быстрый интернет. &a&l(Относится к сетям 4G)"));
                        return;
                    }

                    // Парсим конфиг
                    List<CellularNetworkConfig.Config> configs = CellularNetworkConfig.ParseParams(args[1]);

                    // Получаем предмет в руке
                    ItemStack itemInHand = ((Player) sender).getItemInHand();

                    // Получаем Meta и Data Container предмета в руке
                    ItemMeta metaInHand = itemInHand.getItemMeta();
                    if (metaInHand == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lВозникла ошибка: ItemMeta is null."));
                        return;
                    }

                    PersistentDataContainer dataContainer = metaInHand.getPersistentDataContainer();

                    // Выставляем имя телефона
                    metaInHand.setDisplayName(ChatColor.translateAlternateColorCodes('&', args[0]));

                    // Информация для ЛОООРа
                    CellularNetworkConfig.Generation mainPhoneGeneration = null; // Основное поколение сети телефона
                    CellularNetworkConfig.SimName mainSimName = null; // Имя симки телефона

                    // Итерация по конфигам
                    for(CellularNetworkConfig.Config config : configs) {
                        switch (config){
                            case CellularNetworkConfig.Mcc supported_mcc -> {
                                int[] array = supported_mcc.allowMccList.stream()
                                        .mapToInt(Integer::intValue)
                                        .toArray();

                                dataContainer.set(new NamespacedKey(getPlugin(), "s_mcc"),
                                        PersistentDataType.INTEGER_ARRAY, array);
                            }

                            case CellularNetworkConfig.Mnc supported_mnc -> {
                                int[] array = supported_mnc.allowMncList.stream()
                                        .mapToInt(Integer::intValue)
                                        .toArray();

                                dataContainer.set(new NamespacedKey(getPlugin(), "s_mnc"),
                                        PersistentDataType.INTEGER_ARRAY, array);
                            }

                            case CellularNetworkConfig.NetworkGenerations supported_networks -> {
                                String str = "";

                                for (CellularNetworkConfig.Generation generation: supported_networks.supportedGenerations){
                                    str += generation.toStr() + ";";
                                }

                                mainPhoneGeneration = supported_networks.getFastestGeneration();

                                // e.t.c: gsm;edge;hspa;lte
                                dataContainer.set(new NamespacedKey(getPlugin(), "s_nets"),
                                        PersistentDataType.STRING, str);
                            }

                            case CellularNetworkConfig.SimName simName -> mainSimName = simName;

                            case CellularNetworkConfig.RoamingPolicyAllow ignored -> {
                                dataContainer.set(new NamespacedKey(getPlugin(), "roaming_policy_allow"),
                                        PersistentDataType.BOOLEAN, true);
                            }

                            default -> throw new IllegalStateException("Unexpected value: " + config);
                        }
                    }

                    // Создаем ЛООООООР
                    List<String> phoneLore = new ArrayList<>();

                    if (mainPhoneGeneration != null) {
                        phoneLore.add(ChatColor.translateAlternateColorCodes('&',
                                "&7Network-Gen: " + mainPhoneGeneration.description));
                    }

                    if (mainSimName != null) {
                        phoneLore.add(ChatColor.translateAlternateColorCodes('&',
                                "&7SIM: " + mainSimName.name));
                    }

                    metaInHand.setLore(phoneLore);

                    // Активируем телефон
                    dataContainer.set(new NamespacedKey(getPlugin(), "phone_is_active"),
                            PersistentDataType.BOOLEAN, true);

                    // Мб не нужно, но на всякий случай применяем мету к item-у.
                    itemInHand.setItemMeta(metaInHand);

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&lТелефон создан!"));

                    return;
                }

                // Деактивирует телефон
                if (command.getName().equalsIgnoreCase("delPhone")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    if (!sender.isOp()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lУ вас нет прав для использования этой команды!"));
                        return;
                    }

                    // Получаем предмет в руке
                    ItemStack itemInHand = ((Player) sender).getItemInHand();

                    // Получаем Meta и Data Container предмета в руке
                    ItemMeta metaInHand = itemInHand.getItemMeta();
                    if (metaInHand == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lВозникла ошибка: ItemMeta is null."));
                        return;
                    }

                    PersistentDataContainer dataContainer = metaInHand.getPersistentDataContainer();

                    // Деактивируем телефон
                    dataContainer.set(new NamespacedKey(getPlugin(), "phone_is_active"),
                            PersistentDataType.BOOLEAN, false);

                    // Мб не нужно, но на всякий случай
                    itemInHand.setItemMeta(metaInHand);
                    return;
                }

                // Информация о телефоне
                if (command.getName().equalsIgnoreCase("infoPhone")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    // Получаем предмет в руке
                    ItemStack itemInHand = ((Player) sender).getItemInHand();

                    // Получаем мету и Data Container
                    ItemMeta metaInHand = itemInHand.getItemMeta();
                    if (metaInHand == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lВозникла ошибка: ItemMeta is null."));
                        return;
                    }

                    // Извлекаем инфу о телефоне
                    Phone phone = Phone.getPhoneData(getPlugin(), metaInHand);

                    // Проверяем, а телефон ли это и активирован ли
                    if (phone == null){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lВозникла ошибка: Либо вы держите в руке не телефон, либо он деактивирован."));
                        return;
                    }

                    // MCC
                    StringJoiner mccStrJoiner = new StringJoiner(", ");
                    mccStrJoiner.add("Поддерживаемые MCC:");
                    for (Integer mcc : phone.supportMcc) {
                        mccStrJoiner.add(mcc.toString());
                    }
                    String mccSupports = mccStrJoiner.toString();

                    // MNC
                    StringJoiner mncStrJoiner = new StringJoiner(", ");
                    mncStrJoiner.add("Поддерживаемые MNC:");
                    for (Integer mnc : phone.supportMnc) {
                        mncStrJoiner.add(mnc.toString());
                    }
                    String mncSupports = mncStrJoiner.toString();

                    // Supported Networks
                    StringJoiner networkStrJoiner = new StringJoiner(", ");
                    networkStrJoiner.add("Поддерживаемые сети:");
                    for (CellularNetworkConfig.Generation network : phone.supportNetworks) {
                        networkStrJoiner.add(network.toStr());
                    }
                    String networkSupports = networkStrJoiner.toString();

                    // Roaming Policy
                    String roamingPolicy = "";
                    if (phone.allowRoamingPolicy) {
                        roamingPolicy = "\n  Устройство может подключатся в роуминге.";
                    }

                    // Send message
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&lИнформация о телефоне:&r&l" +
                                    "\n  "+mccSupports+
                                    "\n  "+mncSupports+
                                    "\n  "+networkSupports + roamingPolicy));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lПроизошла ошибка!"));
            }
        });
        return true;
    }

    public static boolean isNum(String strNum) {
        try {
            Integer.parseInt(strNum);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void getPhoneFromItem(ItemStack itemInHand, AtomicReference<Phone> phoneReference) {
        if (itemInHand.getType().isAir()) {
            return;
        }

        var itemMeta = itemInHand.getItemMeta();
        if (itemMeta == null){
            return;
        }

        if (phoneReference.get() != null && itemInHand.equals(phoneReference.get().itemStack)) {
            return;
        }

        var newPhone = Phone.getPhoneData(getPlugin(), itemMeta);
        if (newPhone == null) {
            return;
        }

        newPhone.itemStack = itemInHand;
        phoneReference.set(newPhone);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Инициализируем игрока в playerDb
        PlayerData playerData = playerDb.loadPlayer(player);
        playerData.init(player);

        // Ячейки для старых координатов игрока
        final int[] oldPlayerCords = {0, 0, 0};

        AtomicReference<Phone> phoneInPlayerHand = new AtomicReference<>(null);

        // Runnable таска
        BukkitRunnable br = new BukkitRunnable() {
            @Override
            public void run() {

                // Если игрок при новой итерации отключился
                if (!player.isOnline()) {

                    // Отменяем таску
                    this.cancel();

                    // Чистим кеш боссбаров
                    playerData.bossBarsTempData.clear();

                    // Деинициализируем
                    playerData.deInit();
                    return;
                }

                // Получаем телефон в руке игрока
                getPhoneFromItem(player.getInventory().getItemInMainHand(), phoneInPlayerHand);

                // Если телефона в руке нет - то незачем идти дальше
                if (phoneInPlayerHand.get() == null) {
                    return;
                }

                // Получаем локацию
                var playerLocation = player.getLocation().clone();

                // Если игрок стоит на месте, то есть координаты одни и те же то скипаем итерацию
                if (oldPlayerCords[0] == (int) playerLocation.getX() &&
                        oldPlayerCords[1] == (int) playerLocation.getY() &&
                        oldPlayerCords[2] == (int) playerLocation.getZ()) return;

                // Иначе записываем новые корды
                oldPlayerCords[0] = (int) playerLocation.getX();
                oldPlayerCords[1] = (int) playerLocation.getY();
                oldPlayerCords[2] = (int) playerLocation.getZ();

                // Считаем уровень сигнала от головы игрока
                var playerLocationOfHead = playerLocation.clone();
                playerLocationOfHead.setY(playerLocation.getY() + 1);

                // Ищем ближайшие соты
                var stations_list = stationDb.nearStations(playerLocationOfHead, station -> {
                    // Тут - пред-обработка, она выполняется до того как мы рассчитаем уровень сигнала,
                    // так-как это затратная операция - лучше сначала отсечь все нерелевантные станции.
                    // return true - уровень сигнала рассчитывается и сота попадает в дальнейшую обработку,
                    // return false - соответственно мы отсекаем соту.

                    var phone = phoneInPlayerHand.get();

                    // Если телефона в руке нет, то соты нам не нужны
                    // ну так, на всякий случай проверочка
                    if (phone == null) {
                        return false;
                    }

                    // Если в параметрах юзера отключены FM Станции со сразу скипаем
                    if (playerData.paramOffRadio.get() && station instanceof FMStation)
                        return false;

                    if (station instanceof MobileBaseStation mobileBaseStation) {

                        // Если в параметрах юзера отключены мобильные сети - то скипаем
                        if (playerData.paramOffMobile.get()) {
                            return false;
                        }

                        // Если телефон не поддерживает поколение соты, то зачем ее рассчитывать?
                        if (phone.supportNetworks.contains(mobileBaseStation.generation)){
                            return false;
                        }

                        // Если и сота и телефон поддерживают роуминг - то сразу пропускаем ее
                        if (phone.allowRoamingPolicy && mobileBaseStation.allowIncomingRoaming) {
                            return true;
                        }

                        // Иначе ищем нужный нам mcc
                        if (Collections.disjoint(phone.supportMcc, mobileBaseStation.supportMcc)) {
                            return false;
                        }

                        // и mnc
                        return !Collections.disjoint(phone.supportMnc, mobileBaseStation.supportMnc);
                    }

                    // Если в параметрах юзера отключены TV Станции - то скипаем
                    if (playerData.paramOffTv.get() && station instanceof TVStation)
                        return false;

                    // Если в параметрах юзера отключены Wi-Fi сети - то скипаем
                    if (playerData.paramOffWifi.get() && station instanceof WifiStation)
                        return false;

                    // Если ни один тригер не сработал то.. ну.. пускаем дальше, а что еще делать
                    return true;
                });

                float lastNetworkGenerationWeight = 0;
                Boolean lastNetworkIsRoaming = null;

                List<Long> changedBossBars = new ArrayList<>();
                for (StationDb.NearStationResult stationResult: stations_list) {
                    // Вот тут уже пост-обработка,
                    // после того, как был рассчитан уровень сигнала каждой станции,
                    // то есть тут уже известны уровни сигналов каждой итерируемой соты.

                    // Получаем боссбар из кеша, в кеше они сохраняются по ID-шнику станции,
                    // и кеш этот нужен чтобы плавно управлять уровнем прогресса в зависимости
                    // от сигнала станции.

                    // ______________________________
                    // Вот тут я конкретно запутался во всем, поэтому этот участок кода далко не конечный вариант

                    BossBar bossBarOfStation = null;

                    // FM Station
                    if (stationResult.station instanceof FMStation fmStation) {
                        BossBar bossBarFromCache = playerData.bossBarsTempData.get(stationResult.station.id);

                        changedBossBars.add(stationResult.station.id);

                        // Если боссбара в кеше нету, создаем новый
                        if (bossBarFromCache == null) {
                            bossBarOfStation = getServer().createBossBar( fmStation.name + " (" + fmStation.frequency + "MHz)", BarColor.BLUE, BarStyle.SOLID);
                            playerData.bossBarsTempData.put(stationResult.station.id, bossBarOfStation);

                            bossBarOfStation.setProgress(0);
                            bossBarOfStation.addPlayer(player);
                        } else {
                            bossBarOfStation = bossBarFromCache;
                        }
                    }

                    // Mobile Base Station
                    if (stationResult.station instanceof MobileBaseStation mobileBaseStation) {
                        BossBar bossBarFromCache = playerData.bossBarsTempData.get(1L);

                        // Скипаем если вес generation меньше чем у предыдущей соты,
                        // нам не нужно подключатся к более нисшевой (условно 2G) соте если есть более современная (условно 3G)
                        if (mobileBaseStation.generation.networkGeneration <= lastNetworkGenerationWeight) {
                            continue;
                        }


                        if (Collections.disjoint(phoneInPlayerHand.get().supportMcc, mobileBaseStation.supportMcc)
                            || Collections.disjoint(phoneInPlayerHand.get().supportMnc, mobileBaseStation.supportMnc)) {
                            lastNetworkIsRoaming = true;
                        }

                        // Устанавливаем вес
                        lastNetworkGenerationWeight = mobileBaseStation.generation.networkGeneration;

                        changedBossBars.add(1L);

                        // Если боссбара в кеше нету, создаем новый
                        if (bossBarFromCache == null) {

                            bossBarOfStation = getServer().createBossBar(
                                    mobileBaseStation.name+" ("+mobileBaseStation.generation.displayName+")", // OperatorName (Generation)
                                    mobileBaseStation.generation.toBarColor(), BarStyle.SEGMENTED_6);

                            playerData.bossBarsTempData.put(stationResult.station.id, bossBarOfStation);

                            bossBarOfStation.setProgress(0);
                            bossBarOfStation.addPlayer(player);
                        } else {
                            bossBarOfStation = bossBarFromCache;
                        }
                    }

                    // Если по каким-то причинам боссбар все еще null - то ну.. хз, continue
                    if (bossBarOfStation == null) {
                        continue;
                    }

                    // Дополнительные проверочки
                    if (stationResult.signalPrecent > 100){
                        stationResult.signalPrecent = 100;
                    } else if (stationResult.signalPrecent < 1){
                        continue;
                    }

                    // Ставим прогресс, боссбар требует от 0.0 до 1.0, поэтому делим проценты на 100.
                    bossBarOfStation.setProgress(stationResult.signalPrecent / 100.0);
                }

                // Чистим лишние боссбары у игрока
                playerData.bossBarsTempData.entrySet().removeIf(entry -> {
                    if (changedBossBars.contains(entry.getKey())) {
                        return false;
                    }

                    entry.getValue().removePlayer(player);
                    return true;
                });
            }
        };

        // Запускаем каждые 10 тиков
        br.runTaskTimerAsynchronously(getPlugin(), 20L, 20L);

        // Все что ниже закомичено - это старая архитектура
//        final int[] cords = {0, 0, 0};
//
//        final JSONObject[] plrSets = {new JSONObject()};
//        final ConcurrentHashMap<Integer, BossBar> BSBMap = new ConcurrentHashMap<>();
//
//        ThreadPool.execute(new Runnable() {
//            @Override
//            public void run() {
//                if (!if_file_exs("Sotas" + File.separator + event.getPlayer().getName() + ".json")) {
//                    plrSets[0].put("off_mobile_base_stations", false);
//                    plrSets[0].put("off_tv", false);
//                    plrSets[0].put("off_radio", false);
//                    plrSets[0].put("off_wifi", false);
//                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
//                            "&a&lПривет! &e&lДля помощи по сотам используй &r&l&n/helpSota"));
//                    file_put_contents("Sotas" + File.separator + event.getPlayer().getName() + ".json", plrSets[0].toString());
//                } else {
//
//                    try {
//                        plrSets[0] = new JSONObject(file_get_contents("Sotas" + File.separator + event.getPlayer().getName() + ".json"));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        //0 - Skip, 1 - Ok, 2 - Stop;
//        SotaWork script = new SotaWork() {
//            @Override
//            public int work() {
//                if (!event.getPlayer().isOnline()) return 2;
//
//                final Boolean[] reInitApiLock = {false};
//                ThreadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        PlayerSotasInfo.get(event.getPlayer()).reInit();
//                        reInitApiLock[0] = true;
//                        synchronized (reInitApiLock) {
//                            reInitApiLock.notifyAll();
//                        }
//                    }
//                });
//
//                var location = event.getPlayer().getLocation();
//
//                if (cords[0] == (int) location.getX() &&
//                        cords[1] == (int) location.getY() &&
//                        cords[2] == (int) location.getZ()) return 0;
//
//                cords[0] = (int) location.getX();
//                cords[1] = (int) location.getY();
//                cords[2] = (int) location.getZ();
//
//                ThreadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            if (!if_file_exs("Sotas" + File.separator + event.getPlayer().getName() + ".json")) {
//                                plrSets[0].put("operator", "none");
//                                plrSets[0].put("offmob", false);
//                                plrSets[0].put("optest", true);
//                                plrSets[0].put("offtv", false);
//                                plrSets[0].put("offradio", false);
//                                plrSets[0].put("offwifi", false);
//                                file_put_contents("Sotas" + File.separator + event.getPlayer().getName() + ".json", plrSets[0].toString());
//                            } else {
//                                plrSets[0] = new JSONObject(file_get_contents("Sotas" + File.separator + event.getPlayer().getName() + ".json"));
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//                int net = 0;
//
//                double[] netprec = new double[4];
//                BossBar bsnet = null;
//
//                if (plrSets[0].getBoolean("optest"))
//                    bsnet = getServer().createBossBar("n", BarColor.BLUE, BarStyle.SOLID);
//
//                ItemStack is = event.getPlayer().getItemInHand();
//                if (!getTag(is, "ph").equalsIgnoreCase("1")) {
//                    for (Map.Entry<Integer, BossBar> entry : BSBMap.entrySet()) entry.getValue().removeAll();
//                    BSBMap.clear();
//                    return 1;
//                }
//
//                String currentoperator = plrSets[0].getString("operator");
//
//                String phoneOptag = getTag(is, "op");
//
//                if (!phoneOptag.equals("n")) currentoperator = phoneOptag;
//
//                int maxnet = Integer.parseInt(getTag(is, "mn"));
//
//                ConcurrentHashMap<Integer, BossBar> BossbarsForAdd = new ConcurrentHashMap<>();
//
//                if (!reInitApiLock[0]) synchronized (reInitApiLock) {
//                    try {
//                        reInitApiLock.wait();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//
//                Sota networkSota = null;
//
//                for (Sota sota : SotasList) {
//                    try {
//                        try {
//                            if (sota.Description.equals("OFF")) {
//                                SotasList.remove(sota);
//                                continue;
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            continue;
//                        }
//
//                        BarStyle BStyle = BarStyle.SOLID;
//                        String st = sota.Type;
//                        BarColor bc = BarColor.BLUE;
//
//                        if (st.equalsIgnoreCase("wifi")) {
//                            st = "WiFi";
//                            bc = BarColor.WHITE;
//                            BStyle = BarStyle.SEGMENTED_6;
//                        } else if (st.equalsIgnoreCase("wifi5")) {
//                            st = "WiFi-5G";
//                            bc = BarColor.WHITE;
//                            BStyle = BarStyle.SEGMENTED_10;
//                        } else if (st.equalsIgnoreCase("wifi6")) {
//                            st = "WiFi-6";
//                            bc = BarColor.WHITE;
//                            BStyle = BarStyle.SEGMENTED_10;
//                        } else if (st.equalsIgnoreCase("wifi7")) {
//                            st = "WiFi-7";
//                            bc = BarColor.WHITE;
//                            BStyle = BarStyle.SEGMENTED_10;
//                        } else if (st.equalsIgnoreCase("GSM")) {
//                            if (plrSets[0].getBoolean("offmob")) continue;
//                            if (maxnet < 1) continue;
//
//                            bc = BarColor.RED;
//                            st = "GSM";
//
//                            if (plrSets[0].getBoolean("optest")) {
//                                if (bsnet == null) continue;
//                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
//                                if (net > 1) continue;
//
//                                double prec = SotaSignalPrecent(location, sota);
//                                if (prec <= 0) continue;
//
//                                bsnet.setColor(BarColor.RED);
//                                bsnet.setStyle(BarStyle.SEGMENTED_6);
//                                bsnet.setTitle(sota.Name + " (GSM)");
//
//                                double precforbs = prec / 100.0;
//                                if (precforbs > 1.0) precforbs /= 100.0;
//                                netprec[0] += precforbs;
//                                net = 1;
//
//                                networkSota = sota;
//                                continue;
//                            }
//                        } else if (st.equalsIgnoreCase("EDGE")) {
//                            if (plrSets[0].getBoolean("offmob")) continue;
//                            if (maxnet < 2) continue;
//
//                            bc = BarColor.YELLOW;
//                            st = "EDGE";
//                            BStyle = BarStyle.SEGMENTED_6;
//
//                            if (plrSets[0].getBoolean("optest")) {
//                                if (bsnet == null) continue;
//                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
//                                if (net > 2) continue;
//
//                                double prec = SotaSignalPrecent(location, sota);
//                                if (prec <= 0) continue;
//
//                                bsnet.setColor(BarColor.YELLOW);
//                                bsnet.setStyle(BarStyle.SEGMENTED_6);
//                                bsnet.setTitle(sota.Name + " (EDGE)");
//
//                                double precforbs = prec / 100.0;
//                                if (precforbs > 1.0) precforbs /= 100.0;
//                                netprec[1] += precforbs;
//                                net = 2;
//
//                                networkSota = sota;
//                                continue;
//                            }
//                        } else if (st.equalsIgnoreCase("3G")) {
//                            if (plrSets[0].getBoolean("offmob")) continue;
//                            if (maxnet < 3) continue;
//
//                            bc = BarColor.GREEN;
//                            st = "3G";
//                            BStyle = BarStyle.SEGMENTED_6;
//
//                            if (plrSets[0].getBoolean("optest")) {
//                                if (bsnet == null) continue;
//                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
//                                if (net > 3) continue;
//
//                                double prec = SotaSignalPrecent(location, sota);
//                                if (prec <= 0) continue;
//
//                                bsnet.setColor(BarColor.GREEN);
//                                bsnet.setStyle(BarStyle.SEGMENTED_6);
//                                bsnet.setTitle(sota.Name + " (3G)");
//
//                                double precforbs = prec / 100.0;
//                                if (precforbs > 1.0) precforbs /= 100.0;
//                                netprec[2] += precforbs;
//                                net = 3;
//
//                                networkSota = sota;
//                                continue;
//                            }
//                        } else if (st.equalsIgnoreCase("4G")) {
//                            if (plrSets[0].getBoolean("offmob")) continue;
//                            if (maxnet < 4) continue;
//
//                            bc = BarColor.GREEN;
//                            st = "LTE";
//                            BStyle = BarStyle.SEGMENTED_6;
//
//                            if (plrSets[0].getBoolean("optest")) {
//                                if (bsnet == null) continue;
//                                if (!currentoperator.equalsIgnoreCase(sota.Name)) continue;
//
//                                double prec = SotaSignalPrecent(location, sota);
//                                if (prec <= 0) continue;
//
//                                bsnet.setColor(BarColor.GREEN);
//                                bsnet.setStyle(BarStyle.SEGMENTED_6);
//                                bsnet.setTitle(sota.Name + " (LTE)");
//
//                                double precforbs = prec / 100.0;
//                                if (precforbs > 1.0) precforbs /= 100.0;
//                                netprec[3] += precforbs;
//                                net = 4;
//
//                                networkSota = sota;
//                                continue;
//                            }
//                        } else if (sota.Description.equalsIgnoreCase("TV")) {
//                            if (plrSets[0].getBoolean("offtv")) continue;
//                            st = "TV";
//                        } else if (plrSets[0].getBoolean("offradio")) continue;
//
//                        double prec = SotaSignalPrecent(location, sota);
//                        if (prec <= 0) continue;
//
//                        BossBar bossBar = getServer().createBossBar(sota.Name + " (" + st + ")", bc, BStyle);
//                        double precforbs = prec / 100.0;
//                        if (precforbs > 1.0) precforbs /= 100.0;
//
//                        double finalPrecforbs = precforbs;
//                        ThreadPool.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                PlayerSotasInfo.get(event.getPlayer()).addSota(finalPrecforbs, sota);
//                            }
//                        });
//
//                        bossBar.setProgress(precforbs);
//                        BossbarsForAdd.put(sota.id, bossBar);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                for (Map.Entry<Integer, BossBar> entry : BSBMap.entrySet()) {
//                    if (BossbarsForAdd.get(entry.getKey()) == null) {
//                        entry.getValue().removeAll();
//                        BSBMap.remove(entry.getKey());
//                    }
//                }
//
//                for (Map.Entry<Integer, BossBar> entry : BossbarsForAdd.entrySet()) {
//                    BossBar bs = BSBMap.get(entry.getKey());
//                    if (bs != null) {
//                        bs.setProgress(entry.getValue().getProgress());
//                        if (!bs.getPlayers().contains(event.getPlayer())) bs.addPlayer(event.getPlayer());
//                    } else {
//                        BSBMap.put(entry.getKey(), entry.getValue());
//                    }
//                }
//
//                if (bsnet != null && !plrSets[0].getBoolean("offmob")) {
//                    BossBar finalBsnet = bsnet;
//                    int finalNet = net;
//                    Sota finalNetworkSota = networkSota;
//                    ThreadPool.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//
//                                if (finalBsnet.getTitle().equals("n")) {
//                                    finalBsnet.setTitle(ChatColor.translateAlternateColorCodes('&', "&7[Phone] Нет сети"));
//                                    finalBsnet.setColor(BarColor.WHITE);
//                                    finalBsnet.setProgress(0);
//                                    finalBsnet.setStyle(BarStyle.SEGMENTED_6);
//                                } else {
//                                    if (netprec[finalNet - 1] > 1.0) netprec[finalNet - 1] = 1.0;
//                                    finalBsnet.setProgress(netprec[finalNet - 1]);
//                                }
//
//                                BossBar bs = BSBMap.get(-900);
//                                if (bs == null) {
//                                    finalBsnet.addPlayer(event.getPlayer());
//                                    BSBMap.put(-900, finalBsnet);
//                                    bs = finalBsnet;
//                                } else {
//                                    bs.setProgress(finalBsnet.getProgress());
//                                    if (!bs.getPlayers().contains(event.getPlayer())) bs.addPlayer(event.getPlayer());
//                                }
//
//                                BossBar finalBs = bs;
//                                ThreadPool.execute(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        PlayerSotasInfo.get(event.getPlayer()).addNetSota(finalBs.getProgress(), finalNetworkSota);
//                                    }
//                                });
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//                }
//                return 0;
//            }
//        };
    }
    public Plugin getPlugin() {
        return this;
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent evt) {
//
//    }

}