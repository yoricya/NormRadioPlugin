package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.JSONObject;

public class Sota {

    public String Name;
    public String Type;
    public Float Wats;
    public String Description;
    public int X;
    public int Y;
    public int Z;
    private JSONObject Sotas;

    public Sota(JSONObject sotas){
        Sotas = sotas;
    }
    public Sota(String sota, JSONObject sotas){
        Sotas = sotas;
        String[] sot = sota.split("_");
        if(sot.length < 6){
            System.err.println("Error on Sota.java! 0x1");
            return;
        }
        Name = sot[0];
        Type = sot[1];
        Wats = Float.valueOf(sot[2]);
        Description = sot[3];
        X = Integer.parseInt(sot[4]);
        Y = Integer.parseInt(sot[5]);
        Z = Integer.parseInt(sot[6]);
    }
    public String toString() {
        return Name+"_"+Type+"_"+Wats+"_"+Description+"_"+X+"_"+Y+"_"+Z;
    }

    public boolean newSota(Player pl, String name, String type, Float wats, String desc, JSONObject sotas) {
        X = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockX();
        Y = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockY();
        Z = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockZ();
        Sotas = sotas;
        Name = name;
        Type = type;
        Wats = wats;
        Description = desc;

        return false;
    }
    private int poweredSota(int x, int y, int z, int px, int py, int pz){

        return 0;
    }

}
