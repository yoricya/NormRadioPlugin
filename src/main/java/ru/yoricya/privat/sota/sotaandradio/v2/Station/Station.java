package ru.yoricya.privat.sota.sotaandradio.v2.Station;

import org.bukkit.Location;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;
import static ru.yoricya.privat.sota.sotaandradio.php.rand;

public abstract class Station {
    // Station id
    public long id;

    public @Nullable Integer antennaDirection;

    // Position
    public Location stationLocation;

    // Power in watts
    public double power;

    public abstract String getName();
    public abstract void stationDeserialize(JSONObject jsonObject);

    public static Station Deserialize(JSONObject jsonObject){
        int stationType = jsonObject.getInt("type");
        if(stationType == 0) {
            getLogger().log(Level.WARNING, "Station type cannot be 0, db may be damaged");
            return null;
        }

        // Подбираем тип станции
        Station station = switch (stationType) {
            case 1 -> new FMStation();
            case 2 -> new TVStation();
            case 3 -> new MobileBaseStation();
            case 4 -> new WifiStation();
            default -> null;
        };

        var id = jsonObject.getLong("id");

        if (station == null) {
            getLogger().log(Level.WARNING, "Unknown station (id: " + id + ") type '" + stationType + "', db may be damaged");
            return null;
        }

        // Базовая десериализация
        station.id = id;
        station.power = jsonObject.getDouble("power");

        // Десериализауем локацию станции
        station.stationLocation = new Location(getServer().getWorld(jsonObject.getString("location_w")),
                jsonObject.getDouble("location_x"),
                jsonObject.getDouble("location_y"),
                jsonObject.getDouble("location_z"));

        // Десериализуем направление антенны
        if (jsonObject.has("ant_direction") && jsonObject.get("ant_direction") instanceof Integer antDirection){
            station.antennaDirection = antDirection;
        } else {
            getLogger().log(Level.WARNING, "Cant deserialize antDirection on station (id: " + id + "), db may be damaged");
        }

        // Дальнейшая десереализация
        station.stationDeserialize(jsonObject);

        return station;
    }

    public JSONObject stationSerialize(){
        JSONObject json = new JSONObject();

        // Save id
        json.put("id", id);

        // Save station type
        json.put("type", 0); // Can be overridden at superclass
        // ID:
        // 0 - Not a station
        // 1 - FMStation
        // 2 - TVStation
        // 3 - MobileBaseStation
        // 4 - Wi-Fi Station

        // Save power
        json.put("power", power);

        // Save location data
        json.put("location_x", stationLocation.getX());
        json.put("location_y", stationLocation.getY());
        json.put("location_z", stationLocation.getZ());
        json.put("location_w", stationLocation.getWorld().getName());

        return json;
    }

    // Calculate signal precent
    public int getSignalPrecent(Location player_location) {
        int level = getOfDistanceSignalPrecent(player_location);
        if (level == 0) {
            return 0;
        }

        level += getOfWorldFactorsSignalPrecent();
        return level;
    }
    public int getOfWorldFactorsSignalPrecent() {
        var world = this.stationLocation.getWorld();
        if (world == null) {
            return 0;
        }

        var precent = 0;

        if(world.hasStorm())
            precent -= rand(1, 4);

        if(world.isThundering())
            precent -= rand(2, 5);

        return precent;
    }
    public int getOfDistanceSignalPrecent(Location player_location) {
        int[] point1 = new int[3];
        int[] point2 = new int[3];

        var world = this.stationLocation.getWorld();
        if (world == null) {
            return 0;
        }

        if (!this.stationLocation.getWorld().equals(player_location.getWorld())){
            return 0;
        }

        int level = 100;

        point1[0] = (int) player_location.getX();
        point1[1] = (int) player_location.getY();
        point1[2] = (int) player_location.getZ();

        point2[0] = (int) this.stationLocation.getX();
        point2[1] = (int) this.stationLocation.getY();
        point2[2] = (int) this.stationLocation.getZ();

        // Вычисляем вектор направления диагонали
        int dx = point2[0] - point1[0];
        int dy = point2[1] - point1[1];
        int dz = point2[2] - point1[2];

        // Определяем норму вектора (длину диагонали)
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Вычисляем количество точек на диагонали (с учетом шага 1)
        int steps = (int) Math.round(length);

        level -= (int) ((steps * 2.1) / this.power);

        if (level <= 0) {
            return 0;
        }

        // Вычисляем шаг изменения координат для каждой точки
        float stepX = (float) dx / steps;
        float stepY = (float) dy / steps;
        float stepZ = (float) dz / steps;

        // Проходимся по каждой точке
        for (int i = 0; i <= steps; i++) {
            int x = Math.round(point1[0] + i * stepX);
            int y = Math.round(point1[1] + i * stepY);
            int z = Math.round(point1[2] + i * stepZ);

            float hrdns = StationDb.BlockHardnessCache.get_block_hardness(x, y, z, world); //world.getBlockAt(x, y, z).getType().getHardness();

            if(hrdns == 0)
                continue;

            if(hrdns < 0)
                hrdns = 1000;
            else if(hrdns < 1)
                hrdns = 1;
            else if(hrdns > 100)
                hrdns = 50;

            level -= (int) (hrdns * 12 / this.power);

            if (level <= 0) {
                return 0;
            }
        }

        return level;
    }

}
