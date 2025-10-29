package ru.yoricya.privat.sota.sotaandradio.v2.Phone;


import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.StationsDb;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhonesDb {
    private final List<PhoneData> phoneList = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final File pathToDb;
    public PhonesDb(File pathToDb) {
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
                var data = PhoneData.Deserialize(jsObj);

                if (data != null) {
                    phoneList.add(data);
                } else {
                    Logger.getLogger(StationsDb.class.getName()).log(Level.WARNING, "Can't deserialize phone, because data is null! Phone Db may be damaged?");
                }
            }
        });
    }

    public @Nullable PhoneData getPhoneData(Plugin plugin, ItemMeta itemMeta){
        if (itemMeta == null) {
            return null;
        }

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        // Получаем IMEI из метаданных предмета
        var phoneIsActiveNamespacedKey = new NamespacedKey(plugin, "imei");
        Long imei = container.get(phoneIsActiveNamespacedKey, PersistentDataType.LONG);
        if (imei == null) {
            return null;
        }

        // Если IMEI = 0 то телефон не активен
        if (imei == 0) {
            return null;
        }

        return this.getPhoneData(imei);
    }

    public PhoneData makePhoneData(){
        var random = new Random();

        // Ищем свободный IMEI
        long rndImei;
        do {
            rndImei = random.nextLong();
        } while (getPhoneData(rndImei) != null);

        // Создаем телефон по IMEI
        var phoneData = new PhoneData(rndImei);

        // Сохраняем в бд
        lock.writeLock().lock();
        try {
            phoneList.add(phoneData);
        } finally {
            lock.writeLock().unlock();
        }

        // Возвращаем телефон
        return phoneData;
    }

    public boolean removePhoneData(long imei){
        lock.writeLock().lock();
        try {
            // Удаляем из бд
            return phoneList.removeIf(p -> p.phoneImei == imei);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Nullable PhoneData getPhoneData(long imei){
        lock.readLock().lock();
        try {
            for (PhoneData p : phoneList) {
                if (p.phoneImei == imei) {
                    return p;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return null;
    }

    public void saveDb(){
        JSONArray jsonArr = new JSONArray();

        // Проходимся по каждому телефону сериализуя их в массив
        lock.readLock().lock();
        try {
            for (PhoneData p : phoneList) {
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
