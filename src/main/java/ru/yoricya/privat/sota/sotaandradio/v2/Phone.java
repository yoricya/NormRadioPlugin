package ru.yoricya.privat.sota.sotaandradio.v2;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class Phone {
    public Set<Integer> supportMcc;
    public Set<Integer> supportMnc;
    public Set<CellularNetworkConfig.Generation> supportNetworks = new HashSet<>();
    public boolean allowRoamingPolicy;
    public String phoneName;
    public String simName;
    public ItemStack itemStack;

    // Десериализация телефона из ItemMeta
    public static Phone getPhoneData(Plugin plugin, ItemMeta itemMeta){
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        Phone phone = new Phone();

        // Проверяем активен ли телефон
        var phoneIsActiveNamespacedKey = new NamespacedKey(plugin, "phone_is_active");
        if (!Boolean.TRUE.equals(container.get(phoneIsActiveNamespacedKey, PersistentDataType.BOOLEAN))){
            return null;
        }

        // Получаем все MCC
        var mccNamespacedKey = new NamespacedKey(plugin, "s_mcc");
        if (container.has(mccNamespacedKey, PersistentDataType.INTEGER_ARRAY)) {
            phone.supportMcc = Arrays.stream(container.get(mccNamespacedKey, PersistentDataType.INTEGER_ARRAY))
                    .boxed()
                    .collect(Collectors.toSet());
        }

        // Получаем все MNC
        var mncNamespacedKey = new NamespacedKey(plugin, "s_mnc");
        if (container.has(mncNamespacedKey, PersistentDataType.INTEGER_ARRAY)) {
            phone.supportMnc = Arrays.stream(container.get(mncNamespacedKey, PersistentDataType.INTEGER_ARRAY))
                    .boxed()
                    .collect(Collectors.toSet());
        }

        // Получаем поддерживаемые типы сети
        var networksNamespacedKey = new NamespacedKey(plugin, "s_nets");
        if (container.has(networksNamespacedKey, PersistentDataType.STRING)) {
            String networks = container.get(networksNamespacedKey, PersistentDataType.STRING);

            // Ну на всякий случай
            if (phone.supportNetworks == null) {
                phone.supportNetworks = new HashSet<>();
            }

            // Преобразуем строки в Generation enum
            for(String network : networks.split(";")){
                CellularNetworkConfig.Generation generation = CellularNetworkConfig.Generation.Get(network);
                if (generation == null) {
                    continue;
                }

                phone.supportNetworks.add(generation);
            }
        }

        // Получаем Roaming Policy
        var roamingPolicyAllowNamespacedKey = new NamespacedKey(plugin, "roaming_policy_allow");
        if (container.has(roamingPolicyAllowNamespacedKey, PersistentDataType.BOOLEAN)) {
            phone.allowRoamingPolicy = container.get(roamingPolicyAllowNamespacedKey, PersistentDataType.BOOLEAN);
        }

        return phone;
    }
}
