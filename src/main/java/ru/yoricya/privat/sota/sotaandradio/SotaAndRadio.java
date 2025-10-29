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

import org.bukkit.util.FileUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

import ru.yoricya.privat.sota.sotaandradio.v2.*;
import ru.yoricya.privat.sota.sotaandradio.v2.Phone.PhoneData;
import ru.yoricya.privat.sota.sotaandradio.v2.Phone.PhoneDb;
import ru.yoricya.privat.sota.sotaandradio.v2.Player.PlayerData;
import ru.yoricya.privat.sota.sotaandradio.v2.Player.PlayerDb;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.*;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SotaAndRadio extends JavaPlugin implements Listener {

    // Db's
    public static StationDb stationDb;
    public static PlayerDb playerDb;
    public static PhoneDb phoneDb;

    static final Logger logger = Logger.getLogger("Sota&Radio Plugin");
    public static final AtomicLong currentTick = new AtomicLong(0); // Аналог currentTimeMillis
    static final ForkJoinPool ThreadPool = new ForkJoinPool();

    @Override
    public void onEnable() {
        logger.log(Level.INFO, "Initializing plugin...");
        long startTime = System.currentTimeMillis();

        // Папка бекапов
        new File(getDataFolder(), "stationBackups").mkdirs();

        // Загрузка Дб
        stationsDbLoader(startTime);
        playersDbLoader(startTime);
        phonesDbLoader(startTime);

        if (!getServer().getOnlinePlayers().isEmpty()) for (Player player : getServer().getOnlinePlayers()){
            var playerData = playerDb.loadPlayer(player);
            if (!playerData.isInit()){
                onPlayerJoin(new PlayerJoinEvent(player, null));
            }
        }

        getServer().getScheduler().runTaskTimerAsynchronously(getPlugin(), this::saveAllData, 1000L, 1000L);
        getServer().getPluginManager().registerEvents(this, this);

        // Таймер считающий количество прошедших тиков
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> currentTick.accumulateAndGet(1, Long::sum), 0, 1);

        logger.log(Level.INFO, "Plugin initializing done with "+(System.currentTimeMillis() - startTime)+" ms.");
    }

    void stationsDbLoader(long plStartTime) {
        logger.log(Level.INFO, "Loading stationsDb.");
        long startTime = System.currentTimeMillis();

        var dbFile = new File(getDataFolder(), "stationsDb.json");

        // Backup Db
        if (dbFile.exists()){
            int currentBackupDirName = (int) (plStartTime / 1000 / 60);
            new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName).mkdirs();
            FileUtil.copy(dbFile, new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName + File.separator + "stationsDb.json"));
        }

        // Load Db
        try {
            stationDb = new StationDb(dbFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.log(Level.INFO, "Loading stationsDb done with "+(System.currentTimeMillis() - startTime)+"ms.");
        }
    }

    void playersDbLoader(long plStartTime) {
        logger.log(Level.INFO, "Loading playersDb.");
        long startTime = System.currentTimeMillis();

        var dbFile = new File(getDataFolder(), "playersDb.json");

        // Backup Db
        if (dbFile.exists()){
            int currentBackupDirName = (int) (plStartTime / 1000 / 60);
            new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName).mkdirs();
            FileUtil.copy(dbFile, new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName + File.separator + "playersDb.json"));
        }

        try {
            playerDb = new PlayerDb(dbFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.log(Level.INFO, "Loading playersDb done with "+(System.currentTimeMillis() - startTime)+"ms.");
        }
    }

    void phonesDbLoader(long plStartTime) {
        logger.log(Level.INFO, "Loading phonesDb.");
        long startTime = System.currentTimeMillis();

        var dbFile = new File(getDataFolder(), "phonesDb.json");

        // Backup Db
        if (dbFile.exists()){
            int currentBackupDirName = (int) (plStartTime / 1000 / 60);
            new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName).mkdirs();
            FileUtil.copy(dbFile, new File(getDataFolder(), "stationBackups" + File.separator + currentBackupDirName + File.separator + "phonesDb.json"));
        }

        try {
            phoneDb = new PhoneDb(dbFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            logger.log(Level.INFO, "Loading phonesDb done with "+(System.currentTimeMillis() - startTime)+"ms.");
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
        phoneDb.saveDb();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command,  @NonNull String label, @NonNull String[] args) {
        ThreadPool.execute(() -> {
            if (!command.testPermissionSilent(sender)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cУ вас нет прав для использования этой команды!"));
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
                    switch (stationType) {
                        case "fmradio" -> {

                            if (args.length < 4) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&c&lНе все арги указаны!&r" +
                                                "\n&e   /newSota fmradio <Частота> <Мощность> <Имя> "));
                                return;
                            }

                            double freq;
                            double power;

                            // Парсим значения
                            try {
                                freq = Double.parseDouble(args[1]);
                                power = Double.parseDouble(args[2]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cЧастота и мощность должны быть целым или дробным числом!"));
                                return;
                            }

                            // FM диапазон
                            if (freq < 85 || freq > 108) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cIllegal FM frequency! Min 85Mhz, max 108Mhz."));
                                return;
                            }

                            // Генерируем ID
                            id_of_station = stationDb.randomId();

                            // Создаем станцию
                            FMStation station = new FMStation();
                            station.stationLocation = ((Player) sender).getLocation().clone();
                            station.id = id_of_station;
                            station.power = power;
                            station.frequency = freq;
                            station.name = args[3];

                            // Добавляем станцию
                            stationDb.addStation(station);
                        }
                        case "tv" -> {
                            // Tv Radio - /newSota tv <Freq> <Power> <Name>

                            if (args.length < 4) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&c&lНе все арги указаны!" +
                                                "\n&e   /newSota tv <Частота> <Мощность> <Имя>"));
                                return;
                            }

                            double freq;
                            double power;

                            // Парсим значения
                            try {
                                freq = Double.parseDouble(args[1]);
                                power = Double.parseDouble(args[2]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cЧастота и мощность должны быть целым или дробным числом!"));
                                return;
                            }

                            // Генерируем ID
                            id_of_station = stationDb.randomId();

                            // Создаем станцию
                            TVStation station = new TVStation();
                            station.stationLocation = ((Player) sender).getLocation().clone();
                            station.id = id_of_station;
                            station.power = power;
                            station.frequency = freq;
                            station.name = args[3];

                            // Добавляем станцию
                            stationDb.addStation(station);
                        }
                        case "mobilebs" -> {
                            // Mobile Base Station - /newSota mobilebs <Sota name> <Power> <Options>

                            if (args.length < 4) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&c&lНе все арги указаны!&r" +
                                                "\n&e   /newSota mobilebs <Имя соты> <Мощность> <Опции соты, формата 'key1:val1,val2;key2:val3,val4'> "));
                                return;
                            }

                            double power;

                            // Парсим значения
                            try {
                                power = Double.parseDouble(args[2]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cМощность должна быть целым или дробным числом!"));
                                return;
                            }

                            // Создаем станцию
                            MobileBaseStation mobileBaseSattion = new MobileBaseStation();

                            // Генерируем ID
                            id_of_station = stationDb.randomId();

                            mobileBaseSattion.stationLocation = ((Player) sender).getLocation().clone();
                            mobileBaseSattion.id = id_of_station;
                            mobileBaseSattion.power = power;
                            mobileBaseSattion.name = args[1];

                            // Получаем направление куда смотрит игрок, чтобы сопоставить с направлением антенны (На будущее (Я не уверен, что эта строка правильна, её писал гемини))
                            // P.s это есть только у мобильных сот, так как в ирл, как правило, именно мобильные бс устанавливаются с учетом направления антены
                            // для FM радиостанций направление антены по сути не имеет смысла
                            mobileBaseSattion.antennaDirection = (int) ((((Player) sender).getLocation().getYaw() % 360 + 360) % 360);

                            // Парсим опции
                            for (CellularNetworkConfig.Config config : CellularNetworkConfig.ParseParams(args[3])) {
                                switch (config) {
                                    case CellularNetworkConfig.Mcc mccConfig -> {
                                        mobileBaseSattion.supportMcc = new HashSet<>();
                                        mobileBaseSattion.supportMcc.addAll(mccConfig.allowMccList);
                                    }

                                    case CellularNetworkConfig.Mnc mncConfig -> {
                                        mobileBaseSattion.supportMnc = new HashSet<>();
                                        mobileBaseSattion.supportMnc.addAll(mncConfig.allowMncList);
                                    }

                                    case CellularNetworkConfig.RoamingPolicy roamingPolicy -> {
                                        mobileBaseSattion.allowMccRoaming = roamingPolicy.mccRoaming;
                                        mobileBaseSattion.allowMncRoaming = roamingPolicy.mncRoaming;
                                    }

                                    case CellularNetworkConfig.NetworkGenerations generations -> {
                                        mobileBaseSattion.supportGenerations = new HashSet<>();
                                        mobileBaseSattion.supportGenerations.addAll(generations.supportedGenerations);
                                    }

                                    default -> throw new IllegalStateException("Unexpected value: " + config);
                                }
                            }

                            // Добавляем станцию
                            stationDb.addStation(mobileBaseSattion);
                        }
                    }

                    // Если произошла ошибка
                    if (id_of_station == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lНе все арги указаны!"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', """
                                &aТипы станций:&r
                                   &nwifi&r - Wi-Fi сеть.
                                   &nmobilebs&r - Мобильная Базовая Станция.
                                   &nfmradio&r - FM Радио.
                                   &ntv&r - TV Станция."""));

                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&e/newSota <Тип> ..."));
                        return;
                    }

                    // Если все окей
                    ComponentBuilder cb = new ComponentBuilder();
                    cb.event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(id_of_station)));
                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &aСота создана! ID cоты: " + id_of_station));
                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n  &l&nClick to copy ID."));
                    sender.spigot().sendMessage(cb.build());

                    // Ну и сейвим, да
                    saveAllData();

                    return;
                }

                // Удалить соту
                if (command.getName().equalsIgnoreCase("delSota")) {
                    if (args.length < 1 || !isNum(args[0])) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lНе все аргументы указаны!"));
                        sender.sendMessage("&e/delSota <ID>");
                        return;
                    }

                    var id = Long.parseLong(args[0]);
                    if (stationDb.removeStation(id))
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eСота удалена!"));
                    else
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eСота не существует!"));

                    return;
                }

                // Ближайшие соты
                if (command.getName().equalsIgnoreCase("nearsota")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cЭту команду можно выполнять только от игрока!"));
                        return;
                    }

                    var player_location = ((Player) sender).getLocation().clone();

                    List<StationDb.NearStationResult> listOfStations = stationDb.nearStations(player_location, station -> true);

                    if (listOfStations.isEmpty()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cБлижайших к вам сот нет."));
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
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n&r   Name: "+stationResult.station.getName()));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Signal: "+stationResult.signalPrecent +"%"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Distance: "+Math.round(stationResult.station.stationLocation.distance(player_location)) +" blocks"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Power: "+stationResult.station.power +" w"));
                        cb.append(ChatColor.translateAlternateColorCodes('&',"\n   &l&nClick to copy ID."));

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
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aТекущее значение: " + playerData.paramOffRadio));
                        }
                    } else

                    // Параметр отвечающий за обнаружение ТВ вышек
                    if (param.equalsIgnoreCase("off_tv")) {
                        if (value != null) {
                            playerData.paramOffTv.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aТекущее значение: "+playerData.paramOffTv));
                        }
                    } else

                    // Параметр отвечающий за обнаружение сотовых сетей
                    if (param.equalsIgnoreCase("off_mobile")) {
                        if (value != null) {
                            playerData.paramOffMobile.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aТекущее значение: "+playerData.paramOffMobile));
                        }
                    } else

                    // Параметр отвечающий за частоту FM приемника
                    if (param.equalsIgnoreCase("fm")) {

                        // Получаем данные телефона
                        var phoneData = phoneDb.getPhoneData(getPlugin(), ((Player) sender).getInventory().getItemInMainHand().getItemMeta());

                        // Проверяем есть ли у игрока вообще телефон в руке
                        if (phoneData == null) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eВозьмите в руку телефон."));
                            return;
                        }

                        // Обработка
                        if (value != null) {
                            phoneData.setFmFrequency(Float.parseFloat(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aТекущее значение: " + phoneData.currentFmFrequency.get()));
                        }

                    } else

                    // Параметр отвечающий за обнаружение Wi-Fi сетей
                    if (param.equalsIgnoreCase("off_wifi")) {
                        if (value != null) {
                            playerData.paramOffWifi.set(Boolean.parseBoolean(value));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aПараметр изменен"));
                        } else {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&aТекущее значение: "+playerData.paramOffWifi));
                        }
                    } else

                    // DEBUG Last Iteration Time
                    if (param.equalsIgnoreCase("debug_lit")) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&aТекущее значение: "+playerData.lastIterationTime.get()+"ms"));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                """
                                      &e&lЧто-то не то! Проверьте правильно ли вы вводите команды:&r
                                         &noff_radio <true/false>&r (Отображение радиостанций)
                                         &noff_tv <true/false>&r (Отображение TV станций)
                                         &noff_wifi <true/false>&r (Отображение Wi-Fi сетей)
                                         &noff_mobile <true/false>&r (Отображение мобильных сетей)"""));
                    }

                    return;
                }

                //cmd
                if (command.getName().equalsIgnoreCase("helpSota")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&n/helpSota&r&a - Показать этот список,"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "   &n/userParam&r - Настройка параметров пользователя."));
                    if (sender.isOp()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &6&n/newSota&r - Создать новую соту."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &6&n/delSota&r - Удалить соту"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &6&n/nearSota&r - Найти ближайшую соту."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &9&n/newPhone&r - Создать новый телефон."));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &9&n/delPhone&r - Удалить телефон."));
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &9&n/infoPhone&r - Информация о телефоне."));

                    if (sender.isOp()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &b&n/infoMobileNetwork <id>&r - Информация о соте по ее ID."));
                    }
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "   &b&n/infoMobileNetwork&r - Информация о текущей соте."));

                    //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &d&l&n/newMark&r&l - Создать маркер на динамической карте."));
                    //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', " &d&l&n/delMark&r&l - Удалить маркер на динамической карте."));
                    return;
                }

                // Создает "Телефон" /setPhone <Phone name> <Other params>
                // Other params: net:gsm,gprs,edge,cdma,hspa,hspa+,lte;mcc:250,251;mnc:001,002;allowRoaming:true
                if (command.getName().equalsIgnoreCase("newPhone")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&eЧто-то не то! Проверьте правильно ли вы вводите команды:\n  &n/newPhone <Имя телефона> <Параметры формата 'key1:val1,val2;key2:val3,val4'>"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "   &aВ имени телефона поддерживаются управляющие символы '&n&'&r&a которые делают текст цветным! &cНО ПРОБЕЛЫ НЕ ДОПУСКАЮТСЯ.&r"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "   &cНБТ Теги телефона наложатся на предмет в вашей руке!"));
                        return;
                    }

                    // Парсим конфиг
                    List<CellularNetworkConfig.Config> configs = CellularNetworkConfig.ParseParams(args[1]);

                    // Получаем предмет в руке
                    ItemStack itemInHand = ((Player) sender).getItemInHand();

                    // Получаем Meta и Data Container предмета в руке
                    ItemMeta metaInHand = itemInHand.getItemMeta();
                    if (metaInHand == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cВозникла ошибка: ItemMeta is null."));
                        return;
                    }

                    PersistentDataContainer dataContainer = metaInHand.getPersistentDataContainer();

                    // Выставляем имя телефона
                    metaInHand.setDisplayName(ChatColor.translateAlternateColorCodes('&', args[0]));

                    // Информация для ЛОООРа
                    CellularNetworkConfig.Generation mainPhoneGeneration = null; // Основное поколение сети телефона
                    CellularNetworkConfig.SimName mainSimName = null; // Имя симки телефона

                    // Создаем сам телефон
                    PhoneData phoneData = new PhoneData(0);
                    phoneData.phoneName = args[0];
                    
                    // Итерация по конфигам
                    for(CellularNetworkConfig.Config config : configs) {
                        switch (config){
                            case CellularNetworkConfig.Mcc supported_mcc -> {
                                phoneData.supportMcc = new HashSet<>();
                                phoneData.supportMcc.addAll(supported_mcc.allowMccList);
                            }

                            case CellularNetworkConfig.Mnc supported_mnc -> {
                                phoneData.supportMnc = new HashSet<>();
                                phoneData.supportMnc.addAll(supported_mnc.allowMncList);
                            }

                            case CellularNetworkConfig.NetworkGenerations supported_networks -> {
                                phoneData.supportNetworks = new HashSet<>();
                                phoneData.supportNetworks.addAll(supported_networks.supportedGenerations);
                                mainPhoneGeneration = supported_networks.getFastestGeneration();
                            }

                            case CellularNetworkConfig.SimName simName -> {
                                mainSimName = simName;
                                phoneData.simName = simName.name;
                            }

                            case CellularNetworkConfig.RoamingPolicy roamingPolicy -> {
                                phoneData.allowRoamingPolicy = roamingPolicy.mccRoaming || roamingPolicy.mncRoaming;
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

                    // Создаем телефон в БД
                    PhoneData legalPhone = phoneDb.makePhoneData();
                    legalPhone.fromWithoutImei(phoneData);

                    // Активируем телефон
                    dataContainer.set(new NamespacedKey(getPlugin(), "imei"),
                            PersistentDataType.LONG, legalPhone.phoneImei);

                    // Мб не нужно, но на всякий случай применяем мету к item-у.
                    itemInHand.setItemMeta(metaInHand);

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aТелефон создан!"));

                    return;
                }

                // Не только деактивирует, но еще и удаляет телефон из базы
                if (command.getName().equalsIgnoreCase("delPhone")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    // Получаем предмет в руке
                    ItemStack itemInHand = ((Player) sender).getItemInHand();

                    // Получаем Meta* и Data Container предмета в руке
                    ItemMeta metaInHand = itemInHand.getItemMeta();
                    if (metaInHand == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cВозникла ошибка: ItemMeta is null."));
                        return;
                    }

                    // Получаем контейнер метатегов
                    PersistentDataContainer dataContainer = metaInHand.getPersistentDataContainer();

                    // Получаем IMEI из метатега
                    var phoneIsActiveNamespacedKey = new NamespacedKey(getPlugin(), "imei");
                    Long imei = dataContainer.get(phoneIsActiveNamespacedKey, PersistentDataType.LONG);
                    if (imei == null) {
                        // Если он пустой значит это не телефон
                        return;
                    }

                    // Удаляем ЛООООР
                    metaInHand.setLore(null);

                    // Удаляем из бд
                    phoneDb.removePhoneData(imei);

                    // Удаляем IMEI из метатегов тем самым деактивируя телефон
                    dataContainer.remove(new NamespacedKey(getPlugin(), "imei"));

                    // Мб не нужно, но на всякий случай
                    itemInHand.setItemMeta(metaInHand);

                    // _*Meta является экстремистской организацией на территории Российской Федерации в части продажи продуктов Facebook и Instagram._

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
                    PhoneData phone = phoneDb.getPhoneData(getPlugin(), metaInHand);

                    // Проверяем, а телефон ли это и активирован ли
                    if (phone == null){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lВозникла ошибка: Либо вы держите в руке не телефон, либо он деактивирован."));
                        return;
                    }

                    // MCC
                    String mccSupports = "Поддерживаемые MCC: " + phone.supportMcc.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + ".";

                    // MNC
                    String mncSupports = "Поддерживаемые MNC: " + phone.supportMnc.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + ".";

                    // Supported Networks
                    String networkSupports = "Поддерживаемые сети: " + phone.supportNetworks.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + ".";

                    // Roaming Policy
                    String roamingPolicy = "";
                    if (phone.allowRoamingPolicy) {
                        roamingPolicy = "\n   &eУстройство может подключатся в роуминге.&r";
                    }

                    // Send message
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&lИнформация о телефоне:&r" +
                                    "\n   IMEI: " + phone.phoneImei +
                                    "\n   " + mccSupports +
                                    "\n   " + mncSupports +
                                    "\n   " + networkSupports
                                    + roamingPolicy));
                    return;
                }

                // Информация о соте
                if (command.getName().equalsIgnoreCase("infoMobileNetwork")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Эту команду можно выполнять только от игрока!");
                        return;
                    }

                    // Получаем информацию о соте по ее ID
                    if (args.length > 0) {

                        // Парсим IDшник соты
                        long id;
                        try {
                            id = Long.parseLong(args[0]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cID должен быть целым числом!"));
                            return;
                        }

                        // Получааем саму соту
                        if (stationDb.getStation(id) instanceof MobileBaseStation station) {

                            var text = "&a&lИнформация о соте '" + id + "':&r";

                            text += "\n   Поддержка MCC: " + station.supportMcc.stream()
                                    .map(String::valueOf).collect(Collectors.joining(", ")) + ".";

                            text += "\n   Поддержка MNC: " + station.supportMnc.stream()
                                    .map(String::valueOf).collect(Collectors.joining(", ")) + ".";

                            text += "\n   Поддержка сетей: " + station.supportGenerations.stream()
                                    .map(CellularNetworkConfig.Generation::toStr).collect(Collectors.joining(", ")) + ".";

                            text += "\n   Мощность соты: " + station.power + "w.";

                            text += "\n   Имя сети: " + station.name + ".";

                            if (!station.allowMccRoaming) {
                                text += "\n   &eСота не разрешает международный роуминг.";
                            }

                            if (!station.allowMncRoaming) {
                                text += "\n   &eСота не разрешает внутренний роуминг.";
                            }

                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', text));

                            return;
                        }

                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cСоты с таким ID видимо нет либо это не сота мобильной связи."));

                        return;
                    }

                    // Получаем информацию о текущей соте с телефона

                    // Получаем данные телефона
                    var phoneData = phoneDb.getPhoneData(getPlugin(), ((Player) sender).getInventory().getItemInMainHand().getItemMeta());

                    // Проверяем есть ли у игрока вообще телефон в руке
                    if (phoneData == null) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eВозьмите в руку телефон или передайте ID соты в аргументы команды."));
                        return;
                    }

                    // Получаем информацию о сети
                    var netwInfo = phoneData.getPhoneService(((Player) sender), stationDb);

                    // Проверяем если ли вообще у игрока сеть
                    if (!netwInfo.inService.get()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eУстройство не подключено ни к одной сети."));
                        return;
                    }

                    ComponentBuilder cb = new ComponentBuilder();

                    cb.event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(netwInfo.currentStation.get().id)));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"&a&lИнформация о текущей соте:&r"));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Поддержка MCC: " + netwInfo.currentStation.get().supportMcc.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + "."));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Поддержка MNC: " + netwInfo.currentStation.get().supportMnc.stream()
                            .map(String::valueOf).collect(Collectors.joining(", ")) + "."));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Тип подключения: " + netwInfo.networkGeneration.get().description + "."));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Уровень сети: " + netwInfo.signalStrength.get().intValue() + "%."));

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   Имя сети: " + netwInfo.currentStation.get().name + "."));

                    if (netwInfo.inMccRoaming.get()) {
                        cb.append(ChatColor.translateAlternateColorCodes('&', "\n   &eМеждународный роуминг."));
                    } else if (netwInfo.inMncRoaming.get()) {
                        cb.append(ChatColor.translateAlternateColorCodes('&', "\n   &eВнутренний роуминг."));
                    }

                    if (!netwInfo.currentStation.get().allowMccRoaming) {
                        cb.append(ChatColor.translateAlternateColorCodes('&', "\n   &eСота не разрешает международный роуминг."));
                    }

                    if (!netwInfo.currentStation.get().allowMncRoaming) {
                        cb.append(ChatColor.translateAlternateColorCodes('&', "\n   &eСота не разрешает внутренний роуминг."));
                    }

                    cb.append(ChatColor.translateAlternateColorCodes('&',"\n   &nClick to copy ID '" + netwInfo.currentStation.get().id + "'."));

                    sender.spigot().sendMessage(cb.build());

                    return;
                }

            } catch (Exception e) {
                var tm = (int) (System.currentTimeMillis() / 10000);
                logger.log(Level.WARNING, "Exception error id - '" + tm + "'. ", e);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cПроизошла ошибка! &c&lException error id - '" + tm + "'."));
            }
        });
        return true;
    }

    public static boolean isNum(String strNum) {
        try {
            Long.parseLong(strNum);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Инициализируем игрока в playerDb
        PlayerData playerData = playerDb.loadPlayer(player);
        playerData.init(player);

        // Ячейки для хранения координатов игрока
        final int[] oldPlayerCords = {0, 0, 0};

        // Для проверки завершилась ли задача
        AtomicBoolean isTaskRunning = new AtomicBoolean(false);

        // Предмет в руке игрока
        AtomicReference<ItemStack> itemInPlayerHand = new AtomicReference<>(null);

        // Текущий телефон
        AtomicReference<PhoneData> phoneInPlayerHand = new AtomicReference<>(null);

        // Runnable таска
        BukkitRunnable br = new BukkitRunnable() {
            @Override
            public void run() {
                // Если предыдущая задача не завершилась, то не продолжаем
                if (isTaskRunning.get()) {
                    return;
                }

                // 'Блокируем' выполнение следующей задачи
                isTaskRunning.set(true);

                // Выполняем код внутри
                try{

                    var startTime = System.currentTimeMillis();

                    // ЭТАП 0 (Подготовка) _____________________________________

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

                    // Получаем предмет в руке игрока
                    var item = player.getInventory().getItemInMainHand();
                    if (!item.equals(itemInPlayerHand.get())) {

                        itemInPlayerHand.set(item);

                        // Если игрок взял другой предмет - то парсим этот предмет на наличие метатегов телефона, и ложим в Atomic переменную.
                        phoneInPlayerHand.set(phoneDb.getPhoneData(getPlugin(), item.getItemMeta()));

                    }

                    // Если телефона в руке нет
                    if (phoneInPlayerHand.get() == null) {

                        // Чистим боссбары
                        playerData.bossBarsTempData.entrySet().removeIf(entry -> {
                            entry.getValue().removePlayer(player);
                            return true;
                        });

                        // и незачем идти дальше
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

                    // Получаем локацию игрока
                    var playerLocationOfHead = playerLocation.clone();

                    // Считаем уровень сигнала от головы игрока
                    playerLocationOfHead.setY(playerLocation.getY() + 1);

                    // Mobile station  _____________________________________
                    // Смотрим по параметрам юзера, включены ли сотовые станции
                    // если да то обрабатываем их
                    if (!playerData.paramOffMobile.get()) {

                        // Получаем данные сотовой сети телефона
                        var netwInfo = phoneInPlayerHand.get().getPhoneService(playerLocationOfHead, stationDb);

                        // Получаем боссбар для соты
                        var sotaBossbar = playerData.bossBarsTempData.get(1L);
                        if (sotaBossbar == null) {

                            // Если его нет - то создаем новый
                            sotaBossbar = getServer().createBossBar(ChatColor.translateAlternateColorCodes('&', "&7[Phone] Нет сети")
                                    , BarColor.WHITE, BarStyle.SEGMENTED_6);
                            sotaBossbar.setProgress(0);
                            sotaBossbar.addPlayer(player);

                            // Сохраняем боссбар
                            playerData.bossBarsTempData.put(1L, sotaBossbar);
                        }

                        // Выставляем параметры боссбара для мобильной станции
                        if (netwInfo.inService.get()) {

                            // Основное
                            String title = "&7[Phone] " + netwInfo.networkGeneration.get().displayName;

                            // Отметка роуминга (Если есть)
                            if (netwInfo.inRoaming.get()) {
                                title += "(R)";
                            }

                            // Имя оператора
                            title += "&r - " + netwInfo.currentStation.get().getName();

                            sotaBossbar.setColor(netwInfo.networkGeneration.get().toBarColor());
                            sotaBossbar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
                            sotaBossbar.setProgress(netwInfo.signalStrength.get() / 100);

                        } else {
                            // если сети нет, то нет
                            sotaBossbar.setColor(BarColor.WHITE);
                            sotaBossbar.setTitle(ChatColor.translateAlternateColorCodes('&', "&7[Phone] Нет сети."));
                            sotaBossbar.setProgress(0);
                        }
                    }

                    // FM _____________________________________
                    // Смотрим по параметрам юзера, включены ли FM станции
                    // если да то обрабатываем их
                    if (playerData.paramOffRadio.get()) {

                        // Получаем состояние FM приемника
                        var fmInfo = phoneInPlayerHand.get().getFmStation(playerLocationOfHead, stationDb);

                        // Получаем боссбар для FM
                        var fmBossBar = playerData.bossBarsTempData.get(2L);
                        if (fmBossBar == null) {

                            // Если его нет - то создаем новый
                            fmBossBar = getServer().createBossBar(ChatColor.translateAlternateColorCodes('&', "&7[FM] ////")
                                    , BarColor.BLUE, BarStyle.SOLID);
                            fmBossBar.setProgress(0);
                            // fmBossBar.addPlayer(player);

                            // Сохраняем боссбар
                            playerData.bossBarsTempData.put(2L, fmBossBar);
                        }

                        // Проверяем включен ли приемник
                        if (fmInfo.isReceiverOn.get()) {
                            if (fmBossBar.getPlayers().isEmpty()){
                                fmBossBar.addPlayer(player);
                            }

                            // Если да и есть сигнал - то показываем станцию
                            if (fmInfo.fmStation.get() != null) {
                                String title = "[FM] " + phoneInPlayerHand.get().currentFmFrequency.get() + "MHz: " + fmInfo.fmStation.get().name;
                                fmBossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
                                fmBossBar.setProgress(fmInfo.signalStrength.get() / 100);

                            } else { // Если сигнала нет - то выводим заглушку
                                String title = "[FM] " + phoneInPlayerHand.get().currentFmFrequency.get() + "MHz: ////";
                                fmBossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
                                fmBossBar.setProgress(0);
                            }

                        } else if (!fmBossBar.getPlayers().isEmpty()) { // Если приемник выключен а у пользователя все еще есть боссбар

                            // Удаляем его
                            fmBossBar.removeAll();
                        }
                    }

                    // Вычисляем время итерации
                    playerData.lastIterationTime.set((int) (System.currentTimeMillis() - startTime));

                } finally {
                    // 'Разблокируем' выполнение следующей задачи
                    isTaskRunning.set(false);
                }
            }
        };

        // Запускаем каждые 10 тиков
        br.runTaskTimerAsynchronously(getPlugin(), 5L, 5L);
    }

    public Plugin getPlugin() {
        return this;
    }

}