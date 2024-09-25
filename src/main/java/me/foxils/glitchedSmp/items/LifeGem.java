package me.foxils.glitchedSmp.items;

import me.foxils.foxutils.itemactions.AttackAction;
import me.foxils.foxutils.itemactions.ClickActions;
import me.foxils.foxutils.itemactions.PassiveAction;
import me.foxils.foxutils.utilities.ItemUtils;
import me.foxils.foxutils.utilities.ItemAbility;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.List;

public class LifeGem extends UpgradeableItem implements PassiveAction, AttackAction, ClickActions {

    public final List<PotionEffect> passivePotionEffects = List.of(
            new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false)
    );
    public final PotionEffect witheringEffect = new PotionEffect(PotionEffectType.WITHER, 200, 1, false, true);
    public final PotionEffect powerHeal = new PotionEffect(PotionEffectType.REGENERATION, 100, 4, false, true);

    public final NamespacedKey lifeStealCooldownKey = new NamespacedKey(plugin, "lifegem_lifesteal");
    public final NamespacedKey powerHealCooldownKey = new NamespacedKey(plugin, "lifegem_powerheal");
    public final NamespacedKey witheringCooldownKey = new NamespacedKey(plugin, "lifegem_withering");
    public final NamespacedKey passiveEffectsCooldownKey = new NamespacedKey(plugin, "lifegem_passive_effects");

    public LifeGem(Material material, int customModelData, String name, Plugin plugin, List<ItemAbility> abilityList) {
        super(material, customModelData, name, plugin, abilityList, 3, 0);
    }

    @Override
    public void shiftRightClickAir(PlayerInteractEvent event, ItemStack itemInteracted) {
        powerHeal(event, itemInteracted);
    }

    @Override
    public void shiftRightClickBlock(PlayerInteractEvent event, ItemStack itemInteracted) {
        shiftRightClickAir(event, itemInteracted);
    }

    private void powerHeal(PlayerInteractEvent event, ItemStack itemInteracted) {
        Player player = event.getPlayer();

        if (ItemUtils.getCooldown(powerHealCooldownKey, itemInteracted, 120, player, new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "Used Power-Heal"))) {
            return;
        }

        player.addPotionEffect(powerHeal);
    }

    @Override
    public void rightClickAir(PlayerInteractEvent event, ItemStack itemInteracted) {
        inflictWithering(event, itemInteracted);
    }

    private void inflictWithering(PlayerInteractEvent event, ItemStack itemInteracted) {
        Player playerInflicting = event.getPlayer();

        if (ItemUtils.getCooldown(witheringCooldownKey, itemInteracted, 120, playerInflicting, new TextComponent(ChatColor.GRAY + "" + ChatColor.BOLD + "Used Wither-Away"))) {
            return;
        }

        Entity hitEntity = getEntityLookingAt(playerInflicting);

        if (!(hitEntity instanceof Player hitPlayer)) {
            return;
        }

        hitPlayer.addPotionEffect(witheringEffect);
    }

    @Override
    public void attackAction(EntityDamageByEntityEvent entityDamageByEntityEvent, ItemStack thisItem) {
        tempLifeSteal(entityDamageByEntityEvent, thisItem);
    }

    private void tempLifeSteal(EntityDamageByEntityEvent event, ItemStack thisItem) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof  Player playerAttacked)) {
            return;
        }

        if (ItemUtils.getCooldown(lifeStealCooldownKey, thisItem, 180, player, new TextComponent(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Used Life-Steal"))) {
            return;
        }

        AttributeInstance attckedMaxHealthAttribute = playerAttacked.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance playerMaxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (attckedMaxHealthAttribute == null || playerMaxHealthAttribute == null) return;

        double attackedMaxHealthValue = attckedMaxHealthAttribute.getValue();
        double playerMaxHealthValue = playerMaxHealthAttribute.getValue();

        double attackedNewValue;
        double playerNewValue;

        attackedNewValue = attackedMaxHealthValue - 8;
        playerNewValue = playerMaxHealthValue + 8;

        if (attackedMaxHealthValue <= 4 && attackedMaxHealthValue > 2) {
            attackedNewValue = attackedMaxHealthValue - 4;
            playerNewValue = playerMaxHealthValue + 4;
        }

        attckedMaxHealthAttribute.setBaseValue(attackedNewValue);
        playerMaxHealthAttribute.setBaseValue(playerNewValue);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            attckedMaxHealthAttribute.setBaseValue(attackedMaxHealthValue);
            playerMaxHealthAttribute.setBaseValue(playerMaxHealthValue);
        }, 600);
    }

    @Override
    public void passiveAction(Player player, ItemStack itemStack) {
        giveEffects(player, itemStack);
    }

    private void giveEffects(Player player, ItemStack thisItem) {
        if (ItemUtils.getCooldown(passiveEffectsCooldownKey, thisItem, 10)) {
            return;
        }

        player.addPotionEffects(passivePotionEffects);
    }

    @Nullable
    private LivingEntity getEntityLookingAt(Player player) {
        // I know this is a copy from WaterGem, whatcha gonna do about it (read the top blurb)
        World world = player.getWorld();

        Location eyeLocation = player.getEyeLocation().clone();

        Vector direction = eyeLocation.getDirection().clone();

        RayTraceResult traceResult = world.rayTraceEntities(eyeLocation.add(direction.clone().multiply(0.5)), eyeLocation.getDirection(), 5);

        if (traceResult == null) {
            return null;
        }

        Entity tracedEntity = traceResult.getHitEntity();

        if (!(tracedEntity instanceof LivingEntity livingEntity) || livingEntity.isInvulnerable() || livingEntity.equals(player)) {
            return null;
        }

        return livingEntity;
    }

}
