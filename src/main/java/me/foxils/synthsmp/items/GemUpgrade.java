package me.foxils.synthsmp.items;

import me.foxils.foxutils.Item;
import me.foxils.foxutils.ItemRegistry;
import me.foxils.foxutils.itemactions.ClickActions;
import me.foxils.foxutils.utilities.ItemAbility;
import me.foxils.synthsmp.tables.PlayerStats;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class GemUpgrade extends Item implements ClickActions {

    public GemUpgrade(Material material, String name, Plugin plugin, @NotNull List<ItemAbility> abilityList, List<ItemStack> itemsForRecipe, boolean shapedRecipe) {
        super(material, name, plugin, abilityList, itemsForRecipe, shapedRecipe);
    }

    @Override
    public void rightClickAir(PlayerInteractEvent event, ItemStack itemInteracted) {
        Player player = event.getPlayer();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }

            Item customItemClass = ItemRegistry.getItemFromItemStack(item);

            if (!(customItemClass instanceof UpgradeableItem upgradeableCustomItem)) {
                continue;
            }

            ItemStack upgradeItem = event.getItem();

            if (upgradeItem == null) {
                return;
            }

            int upgradeItemAmount = upgradeItem.getAmount();

            int i;

            for (i = 0; i < upgradeItemAmount; i++) {
                if (!upgradeableCustomItem.upgradeLevel(1, item)) {
                    break;
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1);
            }

            if (i == 0) {
                return;
            }

            upgradeItem.setAmount(upgradeItemAmount - i);

            PlayerStats playerStats = PlayerStats.getDataObjectFromUUID(player.getUniqueId());

            Map<String, Integer> gemLevelMap = playerStats.getGemLevelMap();

            int newLevel = upgradeableCustomItem.getLevel(item);

            if (gemLevelMap.containsKey(upgradeableCustomItem.getRawName())) gemLevelMap.replace(upgradeableCustomItem.getRawName(), newLevel);
            else gemLevelMap.put(upgradeableCustomItem.getRawName(), newLevel);

            playerStats.updateColumn();
            break;
        }
    }

    @Override
    public void rightClickBlock(PlayerInteractEvent event, ItemStack itemInteracted) {
        rightClickAir(event, itemInteracted);
    }
    @Override
    public void shiftRightClickAir(PlayerInteractEvent event, ItemStack itemInteracted) {
        rightClickAir(event, itemInteracted);
    }
    @Override
    public void shiftRightClickBlock(PlayerInteractEvent event, ItemStack itemInteracted) {
        rightClickAir(event, itemInteracted);
    }
}