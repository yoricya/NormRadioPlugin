package ru.yoricya.privat.sota.sotaandradio.v2;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerData {
    @Nullable
    public Player player;
    public HashMap<Long, BossBar> bossBarsTempData = new HashMap<>();

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

        playerData.uuid = jsonObj.getString("uuid");
        playerData.paramOffRadio.set(jsonObj.getBoolean("paramOffRadio"));
        playerData.paramOffTv.set(jsonObj.getBoolean("paramOffTv"));
        playerData.paramOffMobile.set(jsonObj.getBoolean("paramOffMobile"));
        playerData.paramOffWifi.set(jsonObj.getBoolean("paramOffWifi"));

        return playerData;
    }

    // Saveable fields
    public String uuid;
    public AtomicBoolean paramOffRadio = new AtomicBoolean(false);
    public AtomicBoolean paramOffTv = new AtomicBoolean(false);
    public AtomicBoolean paramOffMobile = new AtomicBoolean(false);
    public AtomicBoolean paramOffWifi = new AtomicBoolean(false);
}
