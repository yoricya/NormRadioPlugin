package ru.yoricya.privat.sota.sotaandradio.v2.Phone;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.CellularNetworkConfig;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.FMStation;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.MobileBaseStation;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.StationDb;
import ru.yoricya.privat.sota.sotaandradio.v2.TickCacheRecord;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneData {
    // Serializable
    public Set<Integer> supportMcc = new HashSet<>();
    public Set<Integer> supportMnc = new HashSet<>();
    public Set<CellularNetworkConfig.Generation> supportNetworks = new HashSet<>();
    public boolean allowRoamingPolicy = false;
    public String phoneName = "Default Phone";
    public String simName = "Default Sim";
    public final long phoneImei;
    public float minSupportFmFrequency = 85.5f;
    public float maxSupportFmFrequency = 108f;


    // Another
    public PhoneData(long phoneImei) {
        this.phoneImei = phoneImei;
    }

    public void fromWithoutImei(PhoneData otherPhoneData) {
        this.phoneName = otherPhoneData.phoneName;
        this.simName = otherPhoneData.simName;
        this.supportMcc = otherPhoneData.supportMcc;
        this.supportMnc = otherPhoneData.supportMnc;
        this.supportNetworks = otherPhoneData.supportNetworks;
        this.allowRoamingPolicy = otherPhoneData.allowRoamingPolicy;
        this.minSupportFmFrequency = otherPhoneData.minSupportFmFrequency;
        this.maxSupportFmFrequency = otherPhoneData.maxSupportFmFrequency;
    }

    public JSONObject serialize(){
        JSONObject obj = new JSONObject();

        obj.put("imei", phoneImei);
        obj.put("name", phoneName);
        obj.put("sim_name", simName);
        obj.put("allow_roaming_policy", allowRoamingPolicy);
        obj.put("min_fm_sp_freq", minSupportFmFrequency);
        obj.put("max_fm_sp_freq", maxSupportFmFrequency);

        // Put Mcc's
        var mcc_s = new JSONArray();
        supportMcc.forEach(c -> mcc_s.put(c));
        obj.put("support_mcc", mcc_s);

        // Put Mnc's
        var mnc_s = new JSONArray();
        supportMnc.forEach(c -> mnc_s.put(c));
        obj.put("support_mnc", mnc_s);

        // Put Gen's
        var gen_s = new JSONArray();
        supportNetworks.forEach(c -> gen_s.put(c.toStr()));
        obj.put("support_netw", gen_s);

        return obj;
    }

    public static PhoneData Deserialize(JSONObject jsonObj) {
        PhoneData phoneData;

        // Получаем IMEI
        if (jsonObj.has("imei") && jsonObj.get("imei") instanceof Long imei) {
            phoneData = new PhoneData(imei);
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: N/A - Cannot deserialize IMEI.");

            return null;
        }


        // Получаем имя
        if (jsonObj.has("name") && jsonObj.get("name") instanceof String phoneName) {
            phoneData.phoneName = phoneName;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize phone name.");

            phoneData.phoneName = "Default Phone";
        }


        // Получаем имя симки
        if (jsonObj.has("sim_name") && jsonObj.get("sim_name") instanceof String simName) {
            phoneData.simName = simName;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize sim name.");

            phoneData.simName = "Default SIM";
        }


        // FM Frequencies
        if (jsonObj.has("min_fm_sp_freq") && jsonObj.get("min_fm_sp_freq") instanceof Float min_freq) {
            phoneData.minSupportFmFrequency = min_freq;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize minSupportFmFrequency.");
        }

        if (jsonObj.has("max_fm_sp_freq") && jsonObj.get("max_fm_sp_freq") instanceof Float max_freq) {
            phoneData.maxSupportFmFrequency = max_freq;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize maxSupportFmFrequency.");
        }


        // Получаем политику роуминга
        if (jsonObj.has("allow_roaming_policy") && jsonObj.get("allow_roaming_policy") instanceof Boolean roamingPolicy) {
            phoneData.allowRoamingPolicy = roamingPolicy;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize roaming policy.");

            phoneData.allowRoamingPolicy = false;
        }


        // Получаем MCCs
        phoneData.supportMcc = new HashSet<>();

        var mccJsonArr = jsonObj.getJSONArray("support_mcc");
        mccJsonArr.forEach(obj -> {
            if (obj instanceof Integer mcc) {
                phoneData.supportMcc.add(mcc);
            }
        });


        // Получаем MNCs
        phoneData.supportMnc = new HashSet<>();

        var mncJsonArr = jsonObj.getJSONArray("support_mnc");
        mncJsonArr.forEach(obj -> {
            if (obj instanceof Integer mnc) {
                phoneData.supportMnc.add(mnc);
            }
        });


        // Получаем Generations
        phoneData.supportNetworks = new HashSet<>();

        var netwJsonArr = jsonObj.getJSONArray("support_netw");
        netwJsonArr.forEach(obj -> {
            if (obj instanceof String netw) {
                var gen = CellularNetworkConfig.Generation.Get(netw);
                if (gen == null) {
                    Logger.getLogger(PhoneData.class.getName())
                            .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize network generation '" + netw + "'.");
                    return;
                }

                phoneData.supportNetworks.add(gen);
            }
        });

        return phoneData;
    }

    // Сотовая связь _______________________________
    public TickCacheRecord<NetworkInfo> currentNetworkInfo = new TickCacheRecord<>(20, new NetworkInfo());

    // Получаем кеш либо вычисляем
    public NetworkInfo getPhoneService(Player phoneSender, StationDb stationDb) {
        var location = phoneSender.getLocation().clone();
        location.setY(location.getY() + 1);
        return getPhoneService(location, stationDb);
    }

    public NetworkInfo getPhoneService(Location phoneLocation, StationDb stationDb) {
        return currentNetworkInfo.get((old) -> {
            old.from(searchService(phoneLocation, stationDb));
            return old;
        });
    }

    private NetworkInfo searchService(Location phoneLocation, StationDb stationDb) {

        // Пред-обработка (фильтрация перед расчетом сигнала)
        var nerbyList = stationDb.nearStations(phoneLocation, (station) -> {

            // Если это не мобильная станция, то оно не нужно нам
            if (!(station instanceof MobileBaseStation mobileBaseStation)) {
                return false;
            }

            // Если телефон не поддерживает поколение соты, то зачем ее рассчитывать?
            if (Collections.disjoint(this.supportNetworks, mobileBaseStation.supportGenerations)){
                return false;
            }

            // Если MCC не родной, а сота или телефон не поддерживает роуминг - то отсекаем cоту
            if (Collections.disjoint(this.supportMcc, mobileBaseStation.supportMcc) && !(mobileBaseStation.allowMccRoaming && this.allowRoamingPolicy)) {
                return false;
            }

            // Та же самая проверка, но для MNC
            if (Collections.disjoint(this.supportMnc, mobileBaseStation.supportMnc) && !(mobileBaseStation.allowMncRoaming && this.allowRoamingPolicy)) {
                return false;
            }

            // Если все этапы прошли - поверяем по правилам соты, а можем ли мы в ней вообще зарегистрироваться?
            return mobileBaseStation.canBeRegistered(this);
        });

        // Временная информация о сети
        NetworkInfo tempNetworkInfo = new NetworkInfo();

        // Пост-обработка (выбор лучшей сети после расчета сигнала)
        for (StationDb.NearStationResult nearResult: nerbyList) {

            MobileBaseStation station = (MobileBaseStation) nearResult.station;

            // Расчет необходимости роуминга для данной соты
            boolean isMccDisjoint = Collections.disjoint(this.supportMcc, station.supportMcc);
            boolean isMncDisjoint = Collections.disjoint(this.supportMnc, station.supportMnc);
            boolean isRoamingNeeded = isMccDisjoint || isMncDisjoint; // Нужен ли роуминг для этой новой соты

            // Блок 1: Проверка и отсев, если роуминг невозможен
            if (isRoamingNeeded) {

                if (!this.allowRoamingPolicy) {
                    continue; // Телефон не поддерживает роуминг -> ОТКАЗ
                }

                if (isMccDisjoint && !station.allowMccRoaming) {
                    continue; // Станция не разрешает MCC-роуминг -> ОТКАЗ
                }

                if (isMncDisjoint && !station.allowMncRoaming) {
                    continue; // Станция не разрешает MNC-роуминг -> ОТКАЗ
                }

                // Если мы уже нашли рабочую соту БЕЗ роуминга (tempNetworkInfo.inRoaming.get() == false),
                // то эта (роуминговая) сота нам не нужна, даже если у нее сигнал лучше.
                if (tempNetworkInfo.inService.get() && !tempNetworkInfo.inRoaming.get()) {
                    continue;
                }
            }

            // Сравнение текущей лучшей сети (tempNetworkInfo) с новой сотой
            // Если tempNetworkInfo еще не заполнена (inService == false), то новая сота сразу становится лучшей
            if (tempNetworkInfo.inService.get()) {

                // Если новая сота НЕ требует роуминга (isRoamingNeeded == false),
                // а текущая лучшая сота (tempNetworkInfo) ТРЕБУЕТ роуминга (tempNetworkInfo.inRoaming.get() == true),
                // то новая сота лучше. Мы пропускаем все дальнейшие проверки и переходим к обновлению.
                if (!(!isRoamingNeeded && tempNetworkInfo.inRoaming.get())) {

                    // Получаем поколение текущей лучшей соты
                    float currentGeneration = tempNetworkInfo.networkGeneration.get().networkGeneration;
                    float newGeneration = station.getMaxSupportedGeneration().networkGeneration;

                    // Если у новой соты поколение сети меньше чем у предыдущей
                    if (newGeneration < currentGeneration) {
                        continue; // Скипаем новую
                    }

                    // Если у новой соты поколение сети такое же как у предыдущей
                    if (newGeneration == currentGeneration) {

                        // Но уровень сигнала меньше
                        if (nearResult.signalPrecent < tempNetworkInfo.signalStrength.get()) {

                            // То скипаем
                            continue;
                        }

                    }

                }

            }

            // Если мы дошли до сюда, значит сеть прошла отбор
            tempNetworkInfo.signalStrength.set(nearResult.signalPrecent);
            tempNetworkInfo.currentStation.set(station);
            tempNetworkInfo.networkGeneration.set(station.getMaxSupportedGeneration());
            tempNetworkInfo.inService.set(true);
            tempNetworkInfo.inMccRoaming.set(isMccDisjoint);
            tempNetworkInfo.inMncRoaming.set(isMncDisjoint);
            tempNetworkInfo.inRoaming.set(isRoamingNeeded); // Используем рассчитанное значение
        }

        return tempNetworkInfo;
    }

    // FM Радио _______________________________
    public TickCacheRecord<FMInfo> currentFmStation = new TickCacheRecord<>(20, new FMInfo());
    public final AtomicReference<Float> currentFmFrequency = new AtomicReference<>(0f); // Если 0 - то приемник по сути выключен

    // Выставляем частоту
    public void setFmFrequency(float frequency) throws IllegalArgumentException {
        // Если нолик - то приемник выключен
        if (frequency == 0) {

            // Выставляем нолик тут
            currentFmFrequency.set(0f);

            // А так же ресетим текущую станцию
            currentFmStation.getWithoutCacheMechanism().reset();

            return;
        }

        // Иначе - приемник включен
        currentFmStation.getWithoutCacheMechanism().isReceiverOn.set(true);

        // А значит проверяем диапазоны
        if (frequency < minSupportFmFrequency || frequency > maxSupportFmFrequency) {
            throw new IllegalArgumentException("Invalid frequency: " + frequency + " for support frequency range: " + minSupportFmFrequency + " - " + maxSupportFmFrequency);
        }

        // И выставляем частоту
        currentFmFrequency.set(frequency);
    }

    // Получаем кеш либо вычисляем
    public FMInfo getFmStation(Player phoneSender, StationDb stationDb) {
        var location = phoneSender.getLocation().clone();
        location.setY(location.getY() + 1);
        return getFmStation(location, stationDb);
    }

    public FMInfo getFmStation(Location phoneLocation, StationDb stationDb) {
        return currentFmStation.get((old) -> {

            // Если нолик - то приемник выключен
            if (currentFmFrequency.get() == 0) {

                // а значит ресетим инфо если еще не ресетнуто
                if (old.isReceiverOn.get()) {
                    old.reset();
                }

                // И выходим из функции без расчета
                return old;
            }

            // Иначе делаем вычисления и записываем в инфо новые данные
            old.from(searchFmStation(phoneLocation, stationDb));

            return old;
        });
    }

    // Вычисляем без кеша
    private FMInfo searchFmStation(Location phoneLocation, StationDb stationDb) {

        // Пред-обработка (фильтрация перед расчетом сигнала)
        var nerbyList = stationDb.nearStations(phoneLocation, (station) -> {

            // Если это не FM станция, то оно не нужно нам
            if (!(station instanceof FMStation fmStation)) {
                return false;
            }

            // Если частота станции не входит в поддерживаемый приемником диапазон - то скипаем её
            if (fmStation.frequency < minSupportFmFrequency || fmStation.frequency > maxSupportFmFrequency) {
                return false;
            }

            // Проверяем настроен ли приемник на частоту станции с разбросом в 130 khz, в ирл разброс одной станции +- 130-160 кгц
            return Math.abs(fmStation.frequency - currentFmFrequency.get().doubleValue()) < 0.13;
        });

        // Временный объект для Fm Info
        var tempFmInfo = new FMInfo();
        tempFmInfo.isReceiverOn.set(true);

        // Пост обработка
        for (StationDb.NearStationResult nearResult: nerbyList) {

            // Если предыдущая станция мощнее - то естественно она перебивает сигнал следующей, поэтому скип
            if (tempFmInfo.fmStation.get() != null && tempFmInfo.signalStrength.get() > nearResult.signalPrecent) {
                continue;
            }

            tempFmInfo.fmStation.set((FMStation) nearResult.station);
            tempFmInfo.signalStrength.set(nearResult.signalPrecent);
        }

        return tempFmInfo;
    }

}
