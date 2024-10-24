package me.foxils.scythesmp.commands;

import me.foxils.foxutils.Item;
import me.foxils.foxutils.registry.ItemRegistry;
import me.foxils.scythesmp.items.UpgradeableItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GetItemLevel implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        if (!(commandSender instanceof Player player) || player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            return false;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();

        Item item = ItemRegistry.getItemFromItemStack(itemStack);

        if (!(item instanceof UpgradeableItem upgradeableItem)) {
            return false;
        }

        player.sendMessage(upgradeableItem.getLevel(itemStack) + "");

        return true;
    }
}