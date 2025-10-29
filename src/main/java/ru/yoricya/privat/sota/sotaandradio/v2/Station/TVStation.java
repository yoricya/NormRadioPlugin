package ru.yoricya.privat.sota.sotaandradio.v2.Station;

import org.json.JSONObject;

public class TVStation extends Station {
    public String name;
    public double frequency;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void stationDeserialize(JSONObject jsonObject) {
        name = jsonObject.getString("name");
        frequency = jsonObject.getDouble("freq");
    }

    @Override
    public JSONObject stationSerialize() {
        JSONObject json = super.stationSerialize();

        // Save type
        json.put("type", 2);

        // Save name
        json.put("name", name);

        // Save freq
        json.put("freq", frequency);

        return json;
    }
}
