package ru.yoricya.privat.sota.sotaandradio.v2.Player;


import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.StationsDb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayersDb {
    private final List<PlayerData> playerList = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final File pathToDb;
    public PlayersDb(File pathToDb) {
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

        // Проходимся по массиву десериализуя данные игроков
        jsonArr.forEach(obj -> {
            if (obj instanceof JSONObject jsObj) {
                var data = PlayerData.Deserialize(jsObj);

                if (data != null) {
                    playerList.add(data);
                } else {
                    Logger.getLogger(StationsDb.class.getName()).log(Level.WARNING, "Can't deserialize player, because data is null! Player Db may be damaged?");
                }

            }
        });
    }

    public PlayerData loadPlayer(Player player){
        PlayerData playerData = null;

        lock.readLock().lock();

        try {
            for (PlayerData p : playerList) {
                if (player.getUniqueId().toString().equals(p.uuid)) {
                    playerData = p;
                    break;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        if (playerData == null) {
            playerData = makePlayerData(player);
        }

        return playerData;
    }

    private PlayerData makePlayerData(Player player){
        PlayerData playerData = new PlayerData();

        playerData.player = player;
        playerData.uuid = player.getUniqueId().toString();

        lock.writeLock().lock();

        try {
            playerList.add(playerData);
        } finally {
            lock.writeLock().unlock();
        }

        return playerData;
    }

    public void saveDb(){
        JSONArray jsonArr = new JSONArray();

        // Проходимся по каждой PlayerData сериализуя их в массив
        lock.readLock().lock();
        try {
            for (PlayerData p : playerList) {
                jsonArr.put(p.serialize());
            }
        } finally {
            lock.readLock().unlock();
        }

        // Сохраняем массив в файл
        try {
            Files.writeString(
                    pathToDb.toPath(),
                    jsonArr.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
