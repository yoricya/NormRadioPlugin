package ru.yoricya.privat.sota.sotaandradio.v2;

import org.bukkit.Location;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.BlockHardnessCache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public class StationDb {
    protected static BlockHardnessCache BlockHardnessCache = new BlockHardnessCache();
    private final List<Station> stationList = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final File pathToDb;
    public StationDb(File pathToDb) {
        this.pathToDb = pathToDb;

        // Если файла db нет - создаем
        if (!pathToDb.exists()) {
            try {
                pathToDb.getParentFile().mkdirs();
                pathToDb.createNewFile();
                Files.writeString(pathToDb.toPath(), "[]");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // Читаем из файла строки
        String jsonStr;
        try {
            jsonStr = Files.readString(pathToDb.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Конвертируем их в JSON массив
        JSONArray jsonArr = new JSONArray(jsonStr);

        // Проходимся по массиву десериализуя станции
        jsonArr.forEach(jsObj -> {
            if (jsObj instanceof JSONObject) {
                addStation(Station.Deserialize((JSONObject) jsObj));
            }
        });
    }

    public void addStation(Station station) {
        lock.writeLock().lock();
        stationList.add(station);
        lock.writeLock().unlock();
    }

    public List<NearStationResult> nearStations(Location location, Predicate<Station> filter) {
        var list = new ArrayList<NearStationResult>();

        lock.readLock().lock();
        try{
            for (Station station: stationList){
                // Проверяем подходит ли станция тому кто вызвал функцию
                if (!filter.test(station)) {
                    continue;
                }


                // Проверяем уровень сигнала
                var precent = station.getSignalPrecent(location);
                if (precent == 0) {
                    continue;
                }

                // Добавляем в список результат
                list.add(new NearStationResult(station, precent));
            }

        } finally {
            lock.readLock().unlock();
        }

        return list;
    }

    public boolean removeStation(long id) {
        var station = getStation(id);
        if (station != null) {
            lock.writeLock().lock();

            try {
                stationList.remove(station);
            } finally {
                lock.writeLock().unlock();
            }

            return true;
        }

        return false;
    }

    public Station getStation(long id) {
        lock.readLock().lock();

        try {
            for (Station station : stationList) {
                if (station.id == id) {
                    return station;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return null;
    }

    public long randomId() {
        var random = new Random();

        long id = random.nextLong();
        while(getStation(id) != null) {
            id = random.nextLong();
        }

        return id;
    }

    public void saveDb(){
        var dbJson = new JSONArray();

        // Проходимся по каждой станции сериализуя их в массив
        lock.readLock().lock();
        try {
            for (Station station : stationList) {
                dbJson.put(station.stationSerialize());
            }
        } finally {
            lock.readLock().unlock();
        }

        // Сохраняем массив в файл
        try {
            Files.writeString(
                    pathToDb.toPath(),
                    dbJson.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class NearStationResult {
        public Station station;
        public float signalPrecent;
        NearStationResult(Station station, float precent) {
            this.signalPrecent = precent;
            this.station = station;
        }
    }
}
