package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ru.yoricya.privat.sota.sotaandradio.SotaAndRadio.ThreadPool;

public class BlockHardnessCache {
    private final RWMutexableHashMap<String, RWMutexableHashMap<String, TimeoutedCacheRecord<Float>>> cached_worlds = new RWMutexableHashMap<>();

    public float get_block_hardness(int x, int y, int z, World w){
        var cached_w = cached_worlds.get(w.getName());
        if(cached_w == null){
            cached_worlds.put(w.getName(), new RWMutexableHashMap<>());
            return get_tidy_block_hardness(x, y, z, w);
        }

        var hardness_cached_record = cached_w.get(x+"_"+y+"_"+z);
        if(hardness_cached_record == null){
            return get_tidy_block_hardness(x, y, z, w);
        }

        var hardness = hardness_cached_record.getVal();
        if(hardness == null){
            return get_tidy_block_hardness(x, y, z, w);
        }

        return hardness;
    }

    public float get_tidy_block_hardness(int x, int y, int z, World w){
        Location locPoint = new Location(w, x, y, z);
        Block block = locPoint.getBlock();
        Material type = block.getType();

        float hrdns;
        if (type.isAir()) hrdns = 0f;
        else if (type == Material.WATER) hrdns = 1f;
        else if (type == Material.LAVA) hrdns = 10f;
        else hrdns = type.getHardness();

        ThreadPool.execute(() -> {
            var cached_w = cached_worlds.get(w.getName());
            if(cached_w == null){
                cached_w = new RWMutexableHashMap<>();
                cached_worlds.put(w.getName(), cached_w);
            }

            cached_w.put(x+"_"+y+"_"+z, new TimeoutedCacheRecord<>(5_000, hrdns));
        });

        return hrdns;
    }

    static class RWMutexableHashMap<Ke, V> extends HashMap<Ke, V> {

        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        @Override
        public V put(Ke key, V value) {
            lock.writeLock().lock();
            try {
                return super.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public V get(Object key) {
            lock.readLock().lock();
            try {
                return super.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    static class TimeoutedCacheRecord<T> {
        private T val;

        private final long init_timestamp;

        private final int timeout;

        public TimeoutedCacheRecord(int timeout_in_msecs, T val) {
            this.val = val;
            this.init_timestamp = System.currentTimeMillis();
            this.timeout = timeout_in_msecs;
        }

        public T getVal(){
            if (val == null) return null;

            if(System.currentTimeMillis() - init_timestamp > timeout){
                val = null;
                return null;
            }

            return val;
        }
    }

}
