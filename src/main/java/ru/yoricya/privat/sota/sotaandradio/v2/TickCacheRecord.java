package ru.yoricya.privat.sota.sotaandradio.v2;

import ru.yoricya.privat.sota.sotaandradio.SotaAndRadio;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class TickCacheRecord<T> {
    final AtomicReference<T> atomicT = new AtomicReference<>(null);
    final AtomicLong lastTicks = new AtomicLong(0);
    final float ttl;
    public TickCacheRecord(float ticksToLive, T initial) {
        this.ttl = ticksToLive;
        this.atomicT.set(initial);
    }

    public T get(Function<T, T> functionIfTtlExpired) {
        if (SotaAndRadio.currentTick.get() - lastTicks.get() > ttl || atomicT.get() == null) {
            try {
                return functionIfTtlExpired.apply(atomicT.get());
            } finally {
                lastTicks.set(SotaAndRadio.currentTick.get());
                // System.out.println("Try to cache");
            }
        }

        // System.out.println("Cache worked");

        return atomicT.get();
    }

    public T getWithoutCacheMechanism() {
        return atomicT.get();
    }
}
