package ru.yoricya.privat.sota.sotaandradio.v2;

import org.json.JSONObject;

import java.util.Set;

public class MobileBaseStation extends Station {
    public String name;
    public Set<Integer> supportMcc;
    public Set<Integer> supportMnc;
    public boolean allowIncomingRoaming;
    public CellularNetworkConfig.Generation generation;

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void stationDeserialize(JSONObject jsonObject) {

    }
}
