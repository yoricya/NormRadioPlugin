package ru.yoricya.privat.sota.sotaandradio.v2.Phone;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.CellularNetworkConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneData {
    public Set<Integer> supportMcc = new HashSet<>();
    public Set<Integer> supportMnc = new HashSet<>();
    public Set<CellularNetworkConfig.Generation> supportNetworks = new HashSet<>();
    public boolean allowRoamingPolicy = false;
    public String phoneName = "Default Phone";
    public String simName = "Default Sim";
    public final long phoneImei;

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
    }

    public JSONObject serialize(){
        JSONObject obj = new JSONObject();

        obj.put("imei", phoneImei);
        obj.put("name", phoneName);
        obj.put("sim_name", simName);
        obj.put("allow_roaming_policy", allowRoamingPolicy);

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
        if (jsonObj.get("imei") instanceof Long imei) {
            phoneData = new PhoneData(imei);
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: N/A - Cannot deserialize IMEI.");

            return null;
        }


        // Получаем имя
        if (jsonObj.get("name") instanceof String phoneName) {
            phoneData.phoneName = phoneName;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize phone name.");

            phoneData.phoneName = "Default Phone";
        }


        // Получаем имя симки
        if (jsonObj.get("sim_name") instanceof String simName) {
            phoneData.simName = simName;
        } else {
            Logger.getLogger(PhoneData.class.getName())
                    .log(Level.WARNING, "Phone IMEI: " + phoneData.phoneImei + " - Cannot deserialize sim name.");

            phoneData.simName = "Default SIM";
        }


        // Получаем политику роуминга
        if (jsonObj.get("allow_roaming_policy") instanceof Boolean roamingPolicy) {
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
}
