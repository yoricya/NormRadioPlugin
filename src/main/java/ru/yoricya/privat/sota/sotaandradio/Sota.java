package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import static ru.yoricya.privat.sota.sotaandradio.SotaAndRadio.networkIntGenParse;

public class Sota {

    public String Name;
    public String Type;
    public Float Wats;
    public String Description;
    public int id;
    public int X;
    public int Y;
    public int Z;
    public Sota(){

    }
    public Sota(String sota){
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
        id = Integer.parseInt(sot[7]);
    }

    public double getFrequency(){

        if(Type.equalsIgnoreCase("TV")) return 500;

        if(Type.toLowerCase().contains("tv")){
            try{
                return Double.parseDouble(Type.toLowerCase().replace("tv", ""));
            }catch (NumberFormatException e){
                return 500;
            }
        }

        if(Type.toLowerCase().contains("mhz")){
            try{
                return Double.parseDouble(Type.toLowerCase().replace("mhz", ""));
            }catch (NumberFormatException e){
                return 100;
            }
        }

        if(networkIntGenParse(Type) == 1)
            return 900;

        if(networkIntGenParse(Type) == 2)
            return 900;

        if(networkIntGenParse(Type) == 3)
            return 1900;

        if(networkIntGenParse(Type) == 4)
            return 2100;

        if(networkIntGenParse(Type) == 5)
            return 2100;

        return -1;
    }

    public String toString() {
        return Name+"_"+Type+"_"+Wats+"_"+Description+"_"+X+"_"+Y+"_"+Z+"_"+id;
    }

    public boolean newSota(Player pl, String name, String type, Float wats, String desc) {
        X = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockX();
        Y = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockY();
        Z = SotaAndRadio.Server.getPlayer(pl.getUniqueId()).getLocation().getBlockZ();
        Name = name;
        Type = type;
        Wats = wats;
        Description = desc;

        return true;
    }
    public int getMaxDist(){
        float v = Wats * 75;
        return Math.round(v);
    }

    public int getMidHei(int y){
        int cy = 0;
        if(y > Y){
            int cyi = (y - Y);
            if(cyi == 0){
                return Y;
            }
            cyi /= 2;
            cy = Y +cyi;
        }else{
            int cyi = (Y - y);
            if(cyi == 0){
                return Y;
            }
            cyi /= 2;
            cy = y +cyi;
        }
        return cy;
    }
}
