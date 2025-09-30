package ru.yoricya.privat.sota.sotaandradio.v2.Player;

import ru.yoricya.privat.sota.sotaandradio.v2.CellularNetworkConfig;
import ru.yoricya.privat.sota.sotaandradio.v2.Station.MobileBaseStation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkInfo {
    public AtomicBoolean noService = new AtomicBoolean(true);
    public AtomicBoolean inRoaming = new AtomicBoolean(false);
    public AtomicReference<MobileBaseStation> currentStation = new AtomicReference<>();
    public AtomicReference<Float> signalStrength = new AtomicReference<>(0.0f);
    public AtomicReference<CellularNetworkConfig.Generation> networkGeneration = new AtomicReference<>(null);
    public AtomicBoolean inMccRoaming = new AtomicBoolean(false);
    public AtomicBoolean inMncRoaming = new AtomicBoolean(false);
    public void from(NetworkInfo anotherNetworkInfo) {
        this.noService.set(anotherNetworkInfo.noService.get());
        this.inRoaming.set(anotherNetworkInfo.inRoaming.get());
        this.signalStrength.set(anotherNetworkInfo.signalStrength.get());
        this.currentStation.set(anotherNetworkInfo.currentStation.get());
        this.networkGeneration.set(anotherNetworkInfo.networkGeneration.get());
        this.inMccRoaming.set(anotherNetworkInfo.inMccRoaming.get());
        this.inMncRoaming.set(anotherNetworkInfo.inMncRoaming.get());
    }
    public void reset() {
        this.noService.set(true);
        this.inRoaming.set(false);
        this.signalStrength.set(0.0f);
        this.currentStation.set(null);
        this.networkGeneration.set(null);
        this.inMccRoaming.set(false);
        this.inMncRoaming.set(false);
    }
}
