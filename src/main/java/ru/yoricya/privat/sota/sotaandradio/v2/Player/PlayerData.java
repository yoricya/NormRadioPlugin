package ru.yoricya.privat.sota.sotaandradio.v2.Player;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.Phone.PhoneData;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.MobileBaseStation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerData {
    @Nullable
    public Player player;

    public HashMap<Long, BossBar> bossBarsTempData = new HashMap<>();
    public NetworkInfo networkInfo = new NetworkInfo();
    private final NetworkInfo tempNetworkInfo = new NetworkInfo();
    public AtomicInteger lastIterationTime = new AtomicInteger(-1);

    public void tempAcceptNetStation(MobileBaseStation station, float signalStrength, PhoneData phoneData) {

        // Проверка MCC и MNC
        boolean isMccDisjoint = Collections.disjoint(phoneData.supportMcc, station.supportMcc);
        boolean isMncDisjoint = Collections.disjoint(phoneData.supportMnc, station.supportMnc);

        // Если для подключения нужен роуминг (хотя бы один код не совпал)
        if (isMccDisjoint || isMncDisjoint) {

            // Если есть доступная сота и она без роуминга - то стоп, всегда в приоритете родная сеть
            if (!tempNetworkInfo.noService.get() && !tempNetworkInfo.inRoaming.get()) {
                return;
            }

            // Иначе проверяем разрешение на роуминг

            // Требуется MCC-роуминг, и станция его не разрешает -> ОТКАЗ
            if (isMccDisjoint && !station.allowMccRoaming) {
                return;
            }

            // Требуется MNC-роуминг, и станция его не разрешает -> ОТКАЗ
            if (isMncDisjoint && !station.allowMncRoaming) {
                return;
            }

            // Если телефон не поддерживает роуминг (общая политика), но роуминг требуется -> ОТКАЗ
            if (!phoneData.allowRoamingPolicy) {
                return;
            }

        }


        // Проверяем поколение и уровень сигнала сети
        if (tempNetworkInfo.networkGeneration.get() != null) {

            // Если у новой соты поколение сети меньше чем у предыдущей
            if (station.getMaxSupportedGeneration().networkGeneration < tempNetworkInfo.networkGeneration.get().networkGeneration) {
                // то скипаем новую
                return;
            }

            // Если у новой соты поколение сети такое же как у предыдущей
            if (station.getMaxSupportedGeneration().networkGeneration == tempNetworkInfo.networkGeneration.get().networkGeneration) {
                // Но уровень сигнала меньше (Так же проверяем чтобы это была не одна и та же станция)
                if (signalStrength < networkInfo.signalStrength.get() && station.id != networkInfo.currentStation.get().id) {
                    // то скипаем новую
                    return;
                }
            }

        }


        // Если мы дошли до сюда, значит сеть прошла отбор
        tempNetworkInfo.signalStrength.set(signalStrength);
        tempNetworkInfo.currentStation.set(station);
        tempNetworkInfo.networkGeneration.set(station.getMaxSupportedGeneration());
        tempNetworkInfo.noService.set(false);
        tempNetworkInfo.inMccRoaming.set(isMccDisjoint);
        tempNetworkInfo.inMncRoaming.set(isMncDisjoint);
        tempNetworkInfo.inRoaming.set(isMccDisjoint || isMncDisjoint);
    }

    public void pushNetStation() {
        networkInfo.from(tempNetworkInfo);
        tempNetworkInfo.reset();
    }

    public void init(Player player) {
        this.player = player;
    }

    public void deInit() {
        this.player = null;
    }

    public boolean isInit() {
        return this.player != null;
    }

    public JSONObject serialize(){
        JSONObject obj = new JSONObject();

        obj.put("uuid", uuid);
        obj.put("paramOffRadio", paramOffRadio);
        obj.put("paramOffTv", paramOffTv);
        obj.put("paramOffMobile", paramOffMobile);
        obj.put("paramOffWifi", paramOffWifi);

        return obj;
    }

    public static PlayerData Deserialize(JSONObject jsonObj) {
        PlayerData playerData = new PlayerData();

        try {
            playerData.uuid = jsonObj.getString("uuid");
            playerData.paramOffRadio.set(jsonObj.getBoolean("paramOffRadio"));
            playerData.paramOffTv.set(jsonObj.getBoolean("paramOffTv"));
            playerData.paramOffMobile.set(jsonObj.getBoolean("paramOffMobile"));
            playerData.paramOffWifi.set(jsonObj.getBoolean("paramOffWifi"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return playerData;
    }

    // Saveable fields
    public String uuid;
    public AtomicBoolean paramOffRadio = new AtomicBoolean(false);
    public AtomicBoolean paramOffTv = new AtomicBoolean(false);
    public AtomicBoolean paramOffMobile = new AtomicBoolean(false);
    public AtomicBoolean paramOffWifi = new AtomicBoolean(false);
}
