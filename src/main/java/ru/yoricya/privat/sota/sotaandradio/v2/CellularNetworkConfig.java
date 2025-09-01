package ru.yoricya.privat.sota.sotaandradio.v2;

import org.bukkit.boss.BarColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CellularNetworkConfig {
    public static List<Config> ParseParams(String params) throws IllegalStateException {
        List<Config> configs = new ArrayList<>();

        for (String param: params.trim().split(";")){
            var paramSplited = param.split(":");

            String key = paramSplited[0].trim().toLowerCase();
            String[] values = null;

            if (paramSplited.length > 1){
                values = paramSplited[1].split(",");
            }

            Config config = switch (key){
                case "mcc" -> {
                    if (values == null) {
                        throw new IllegalStateException("Parsing error! Values cant be null!");
                    }

                    Mcc mcc = new Mcc();

                    for (String value: values){
                        mcc.allowMccList.add(Integer.parseInt(value));
                    }

                    yield mcc;
                }

                case "mnc" -> {
                    if (values == null) {
                        throw new IllegalStateException("Parsing error! Values cant be null!");
                    }

                    Mnc mnc = new Mnc();

                    for (String value: values){
                        mnc.allowMncList.add(Integer.parseInt(value));
                    }

                    yield mnc;
                }

                case "gens", "supportgens", "net", "nets", "networks" -> {
                    if (values == null) {
                        throw new IllegalStateException("Parsing error! Values cant be null!");
                    }

                    NetworkGenerations generations = new NetworkGenerations();

                    for (String value: values){
                        Generation generation = Generation.Get(value);

                        if (generation == null){
                            continue;
                        }

                        generations.supportedGenerations.add(generation);
                    }

                    yield generations;
                }

                case "sim", "simname" -> {
                    if (values == null) {
                        throw new IllegalStateException("Parsing error! Values cant be null!");
                    }

                    SimName simName = new SimName();
                    simName.name = values[0];
                    yield simName;
                }

                case "allowroaming" -> new RoamingPolicyAllow();

                default -> throw new IllegalStateException("Unexpected value: " + key);
            };

            configs.add(config);
        }

        return configs;
    }

    public abstract static class Config {}
    public static class Mcc extends Config {
        public List<Integer> allowMccList = new ArrayList<>();
    }
    public static class Mnc extends Config {
        public List<Integer> allowMncList = new ArrayList<>();
    }
    public static class NetworkGenerations extends Config {
        public List<Generation> supportedGenerations = new ArrayList<>();
        public Generation getFastestGeneration() {
            if (this.supportedGenerations.isEmpty()) {
                return null;
            }

            return this.supportedGenerations.stream()
                    .max(Comparator.comparingDouble(gen -> gen.networkGeneration))
                    .orElse(null);
        }
    }
    public static class RoamingPolicyAllow extends Config {}
    public static class SimName extends Config {
        public String name;
    }

    public enum Generation {
        // 2G
        GSM("G", false, "GSM (voice only)", 2.0f),
        GPRS("G+", true,  "GPRS (very slow data)", 2.5f),
        EDGE("E", true,  "EDGE (slow data)", 2.75f),

        // 3G
        CDMA("3G", true, "CDMA (slow data)", 3.0f),
        UMTS("3G", true, "UMTS (basic data)", 3.1f),
        HSPA("H", true, "HSPA (enhanced 3G)", 3.5f),
        HSPA_PLUS("H+", true, "HSPA+ (up to 84 Mbps)", 3.75f),

        // 4G
        LTE("4G", true, "4G (fast data)", 4.0f),
        LTE_ADVANCED("4G+", true, "LTE Advanced (Carrier aggregation)", 4.5f),

        // 5G
        NR("5G", true, "5G (ultra-fast, low latency)", 5.0f),
        NR_ADVANCED("5G+", true, "5G Advanced (standalone, high capacity)", 5.5f);

        public final String displayName;
        public final boolean hasInternet;
        public final String description;
        public final float networkGeneration;

        Generation(String displayName, boolean hasInternet, String description, float networkGeneration) {
            this.displayName = displayName;
            this.hasInternet = hasInternet;
            this.description = description;
            this.networkGeneration = networkGeneration;
        }

        public String toStr() {
            return switch (this) {
                case GSM -> "gsm";
                case GPRS -> "gprs";
                case EDGE -> "edge";
                case CDMA -> "cdma";
                case UMTS -> "umts";
                case HSPA -> "hspa";
                case HSPA_PLUS -> "hspa+";
                case LTE -> "lte";
                case LTE_ADVANCED -> "lte_advanced";
                case NR -> "nr";
                case NR_ADVANCED -> "nr_advanced";
            };
        }

        public BarColor toBarColor() {
            return switch (this) {
                case GSM, GPRS -> BarColor.RED;
                case EDGE -> BarColor.YELLOW;
                case CDMA, UMTS, HSPA, HSPA_PLUS -> BarColor.GREEN;
                case LTE, LTE_ADVANCED, NR, NR_ADVANCED -> BarColor.PURPLE;
            };
        }

        public static Generation Get(String name) {
            return switch (name.trim().toLowerCase()) {
                case "gsm", "g" -> GSM;
                case "gprs", "g+" -> GPRS;
                case "edge", "2g", "e" -> EDGE;
                case "cdma" -> CDMA;
                case "umts" -> UMTS;
                case "hspa", "h" -> HSPA;
                case "hspa+", "h+", "3g" -> HSPA_PLUS;
                case "lte", "4g" -> LTE;
                case "lte_advanced", "lte+" -> LTE_ADVANCED;
                case "nr", "5g" -> NR;
                case "nr_advanced", "nr+" -> NR_ADVANCED;
                default -> null;
            };
        }
    }
}
