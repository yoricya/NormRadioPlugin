package ru.yoricya.privat.sota.sotaandradio.v2.Phone;

import ru.yoricya.privat.sota.sotaandradio.v2.Station.FMStation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FMInfo {
    public final AtomicReference<FMStation> fmStation = new AtomicReference<>();
    public final AtomicReference<Float> signalStrength = new AtomicReference<>(0.0f);
    public final AtomicBoolean isReceiverOn = new AtomicBoolean(false);
    public void from(FMInfo anotherFmInfo) {
        this.fmStation.set(anotherFmInfo.fmStation.get());
        this.signalStrength.set(anotherFmInfo.signalStrength.get());
        this.isReceiverOn.set(anotherFmInfo.isReceiverOn.get());
    }

    public void reset() {
        this.fmStation.set(null);
        this.signalStrength.set(0.0f);
        this.isReceiverOn.set(false);
    }
}
