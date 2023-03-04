package ru.yoricya.privat.sota.sotaandradio;

import org.bukkit.Material;

public class MaterialsCustom {
    public static int GetByMate(Material m){
        if(m == Material.GRASS_BLOCK) return 28;
        if(m == Material.COBBLESTONE) return 49;
        if(m == Material.STONE) return 55;
        if(m == Material.SPRUCE_LOG) return 20;
        if(m == Material.OAK_LOG) return 20;
        if(m == Material.BIRCH_LOG) return 20;
        if(m == Material.BLACK_CONCRETE) return 50;
        if(m == Material.WHITE_CONCRETE) return 50;
        if(m == Material.GRAY_CONCRETE) return 50;
        if(m == Material.LIGHT_GRAY_CONCRETE) return 50;
        if(m == Material.WATER) return 32;
        if(m == Material.GRAVEL) return 29;
        if(m == Material.COAL_ORE) return 50;
        if(m == Material.IRON_ORE) return 105;
        if(m == Material.COPPER_ORE) return 85;
        if(m == Material.GOLD_ORE) return 95;
        if(m == Material.COPPER_BLOCK) return 120;
        if(m == Material.IRON_BLOCK) return 250;
        if(m == Material.OXIDIZED_COPPER) return 105;
        if(m == Material.EXPOSED_COPPER) return 100;
        if(m == Material.WEATHERED_COPPER) return 100;
        if(m == Material.CUT_COPPER) return 100;
        if(m == Material.EXPOSED_CUT_COPPER) return 100;
        if(m == Material.WEATHERED_CUT_COPPER) return 100;
        if(m == Material.OXIDIZED_CUT_COPPER) return 100;
        if(m == Material.QUARTZ_PILLAR) return 45;
        if(m == Material.DARK_PRISMARINE) return 35;
        return 25;
    }
}
