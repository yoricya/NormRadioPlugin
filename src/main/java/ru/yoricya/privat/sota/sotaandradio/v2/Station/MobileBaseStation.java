package ru.yoricya.privat.sota.sotaandradio.v2.Station;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.CellularNetworkConfig;
import ru.yoricya.privat.sota.sotaandradio.v2.Phone.PhoneData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MobileBaseStation extends Station {
    public String name = "Default Name";
    public Set<Integer> supportMcc = new HashSet<>();
    public Set<Integer> supportMnc = new HashSet<>();
    public Set<CellularNetworkConfig.Generation> supportGenerations = new HashSet<>();;
    public boolean allowMccRoaming = false;
    public boolean allowMncRoaming = false;
    public CellularNetworkConfig.Generation getMaxSupportedGeneration() {
        AtomicReference<CellularNetworkConfig.Generation> maxSupportedGeneration = new AtomicReference<>(null);

        supportGenerations.forEach(generation -> {
            if (maxSupportedGeneration.get() == null || generation.networkGeneration > maxSupportedGeneration.get().networkGeneration) {
                maxSupportedGeneration.set(generation);
            }
        });

        return maxSupportedGeneration.get();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void stationDeserialize(JSONObject jsonObject) {
        name = jsonObject.getString("name");
        allowMccRoaming = jsonObject.getBoolean("allow_mcc_roaming");
        allowMncRoaming = jsonObject.getBoolean("allow_mnc_roaming");

        // MCC
        jsonObject.getJSONArray("support_mcc").forEach(obj -> {
            if(obj instanceof Integer mcc) {
                supportMcc.add(mcc);
            }
        });

        if (supportMcc.isEmpty()) {
            Logger.getLogger(MobileBaseStation.class.getName()).log(Level.WARNING, "Can't find any support MCC for station id '" + this.id + "'");
        }

        // MNC
        jsonObject.getJSONArray("support_mnc").forEach(obj -> {
            if(obj instanceof Integer mnc) {
                supportMnc.add(mnc);
            }
        });

        if (supportMnc.isEmpty()) {
            Logger.getLogger(MobileBaseStation.class.getName()).log(Level.WARNING, "Can't find any support MNC for station id '" + this.id + "'");
        }

        // GENS
        jsonObject.getJSONArray("support_netw").forEach(obj -> {
            if(obj instanceof String gen_str) {
                var gen = CellularNetworkConfig.Generation.Get(gen_str);
                if (gen != null) {
                    supportGenerations.add(gen);
                }
            }
        });

        if (supportGenerations.isEmpty()) {
            Logger.getLogger(MobileBaseStation.class.getName()).log(Level.WARNING, "Can't find any support generation for station id '" + this.id + "'");
        }
    }

    @Override
    public JSONObject stationSerialize() {
        JSONObject json = super.stationSerialize();

        json.put("type", 3);

        json.put("name", name);
        json.put("allow_mcc_roaming", allowMccRoaming);
        json.put("allow_mnc_roaming", allowMncRoaming);

        // Save MCC's
        var mcc_s = new JSONArray();
        supportMcc.forEach(c -> mcc_s.put(c));
        json.put("support_mcc", mcc_s);

        // Save MCC's
        var mnc_s = new JSONArray();
        supportMnc.forEach(c -> mnc_s.put(c));
        json.put("support_mnc", mnc_s);

        // Save Generations
        var gens = new JSONArray();
        supportGenerations.forEach(c -> gens.put(c.toStr()));
        json.put("support_netw", gens);

        return json;
    }

    public boolean canBeRegistered(PhoneData phoneData) {
        return true;
    }
}
