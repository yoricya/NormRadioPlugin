package ru.yoricya.privat.sota.sotaandradio.v2.Station;

import org.json.JSONObject;

public class WifiStation extends Station {
    public String Name;
    public WifiBand Band;

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void stationDeserialize(JSONObject jsonObject) {

    }

    public enum WifiBand {
        BAND_2_4,
        BAND_5,
        BAND_6,
    }
}
