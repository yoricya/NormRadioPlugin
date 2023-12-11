package ru.yoricya.privat.sota.sotaandradio;

import java.util.ArrayList;
import java.util.List;

public class SotasInfo {
    final List<SotaInf> availableSotas = new ArrayList<>();
    SotaInf netSota = null;

    void addSota(double precent, Sota sota){
        availableSotas.add(new SotaInf(precent, sota));
    }
    void addNetSota(double precent, Sota sota){
        netSota = new SotaInf(precent, sota);
    }

    void reInit(){
        availableSotas.clear();
    }

    public List<SotaInf> getAvailableSotas(){
        return availableSotas;
    }
    SotaInf getNetSota(){
        return netSota;
    }

}
