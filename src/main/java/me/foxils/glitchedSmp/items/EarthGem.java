package me.foxils.glitchedSmp.items;

import me.foxils.foxutils.Item;
import me.foxils.foxutils.itemactions.*;
import me.foxils.foxutils.utilities.ItemUtils;
import me.foxils.glitchedSmp.GlitchedSmp;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EarthGem extends Item implements MineAction, DropAction, ClickActions, PassiveAction {

    private static final Plugin plugin = GlitchedSmp.getInstance();

    private static final List<List<FallingBlock>> blocksGroupsThrown = new ArrayList<>();
    private static final HashMap<List<FallingBlock>, Player> thrownBlockGroupPlayerMap = new HashMap<>();
    private static List<List<FallingBlock>> toBeRemoved = new ArrayList<>();

    private static final Vector throwVector = new Vector(0, 0.5, 0);
    private static final PotionEffect earthHasteEffect = new PotionEffect(PotionEffectType.HASTE, 200, 1, false, false);

    private static final NamespacedKey miningBoundKey = new NamespacedKey(plugin, "miningBounds");
    private static final NamespacedKey miningBoundsCooldownKey = new NamespacedKey(plugin, "miningBoundsCooldown");

    // Extend base item to add function
    public EarthGem(Material material, String name, NamespacedKey key, List<ItemStack> itemsForRecipe, boolean shapedRecipe) {
        super(material, name, key, itemsForRecipe, shapedRecipe);
    }

    @Override
    public ItemStack createItem(int amount) {
        ItemStack newItem =  super.createItem(amount);

        return ItemUtils.storeDataOfType(PersistentDataType.INTEGER_ARRAY, new int[]{-1, 2}, miningBoundKey, newItem);
    }

    @Override
    public void blockMineAction(BlockBreakEvent event, ItemStack itemUsed, ItemStack thisItem) {
        if (event.getPlayer().isSneaking()) {
            return;
        }

        Block blockBroken = event.getBlock();

        Location blockLocation = blockBroken.getLocation();
        World blockWorld = blockBroken.getWorld();

        //-1, 2

        int[] miningBounds = ItemUtils.getDataOfType(PersistentDataType.INTEGER_ARRAY, miningBoundKey, thisItem);
        int lowerDepth = miningBounds[0];
        int upperDepth = miningBounds[1];

        for (int x = lowerDepth; x < upperDepth; x++) {
            for (int z = lowerDepth; z < upperDepth; z++) {
                for (int y = lowerDepth; y < upperDepth; y++) {
                    Block blockToBreak = blockWorld.getBlockAt((int) (blockLocation.getX() + x), (int) (blockLocation.getY() + y), (int) (blockLocation.getZ() + z));

                    Material blockType = blockToBreak.getType();

                    if (blockType == Material.END_PORTAL || blockType == Material.END_GATEWAY || blockType == Material.BEDROCK || blockType == Material.NETHER_PORTAL || blockType == Material.END_PORTAL_FRAME || blockType == Material.END_CRYSTAL) {
                        continue;
                    }

                    blockToBreak.breakNaturally();
                }
            }
        }
    }

    @Override
    public void dropItemAction(PlayerDropItemEvent event, ItemStack itemUsed) {
        if (ItemUtils.getCooldown(miningBoundsCooldownKey, itemUsed, 900)) {
            return;
        }

        ItemStack item = ItemUtils.storeDataOfType(PersistentDataType.INTEGER_ARRAY, new int[]{-1, 3}, miningBoundKey, itemUsed);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (ItemStack itemStack : event.getPlayer().getInventory().getContents()) {
                    if (itemStack == null || itemStack.getType() == Material.AIR) {
                        continue;
                    }

                    if (!itemStack.isSimilar(item)) {
                        continue;
                    }

                    ItemUtils.storeDataOfType(PersistentDataType.INTEGER_ARRAY, new int[]{-1, 2}, miningBoundKey, itemStack);
                }
            }
        }.runTaskLater(plugin, 100L);

    }

    private void doEarthToss(Player player, ItemStack item) {
        if (ItemUtils.getCooldown(new NamespacedKey(plugin, "earth_toss_cooldown"), item, 900)) {
            return;
        }

        World world = player.getWorld();
        Location playerPosition = player.getLocation();

        List<FallingBlock> blockGroup = new ArrayList<>();

        for (int y = -2; y < 0; y++) {
            if (y == -2) {
                for (int x = -1; x < 2; x++) {
                    if (x == 0) {
                        for (int z = -1; z < 2; z++) {
                            blockGroup.add(createThrowBlock(world, playerPosition.clone().add(x, y, z)));
                        }
                        continue;
                    }

                    blockGroup.add(createThrowBlock(world, playerPosition.clone().add(x, y, 0)));
                }
                continue;
            }
            for (int x = -2; x < 3; x++) {
                if (x > -2 && x < 2) {
                    if (x == 0) {
                        for (int z = -2; z < 3; z++) {
                            blockGroup.add(createThrowBlock(world, playerPosition.clone().add(x, y, z)));
                        }
                        continue;
                    }

                    for (int z = -1; z < 2; z++) {
                        blockGroup.add(createThrowBlock(world, playerPosition.clone().add(x, y, z)));
                    }
                    continue;
                }

                blockGroup.add(createThrowBlock(world, playerPosition.clone().add(x, y, 0)));
            }
        }

        blocksGroupsThrown.add(blockGroup);
        thrownBlockGroupPlayerMap.put(blockGroup, player);
    }

    @Override
    public void rightClickAir(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        doEarthToss(player, item);
    }

    @Override
    public void rightClickBlock(PlayerInteractEvent event) {
        rightClickAir(event);
    }

    private static FallingBlock createThrowBlock(World world, Location location) {
        BlockData blockData = world.getBlockData(location);
        Material blockMaterial = blockData.getMaterial();

        if (blockMaterial == Material.AIR || !blockMaterial.isSolid()) {
            blockData = Material.DIORITE.createBlockData();
        }

        FallingBlock fallingBlock = world.spawnFallingBlock(location.clone().add(0, 2, 0), blockData);

        fallingBlock.setCancelDrop(true);
        fallingBlock.setVelocity(location.getDirection().clone().add(throwVector).multiply(1));

        return fallingBlock;
    }

    private static void removeBlockGroup(List<FallingBlock> blockGroup) {
        blocksGroupsThrown.remove(blockGroup);
        thrownBlockGroupPlayerMap.remove(blockGroup);
        for (FallingBlock block : blockGroup) {
            block.remove();
        }
        Location blockLocation = blockGroup.getFirst().getLocation();
        World blockWorld = blockGroup.getFirst().getWorld();

        blockWorld.spawnParticle(Particle.EXPLOSION_EMITTER, blockLocation, 5, 1, 1, 1);
        blockWorld.playSound(blockLocation, Sound.ENTITY_IRON_GOLEM_DEATH, 1, 0.8F);
    }

    public static void customThrowCollision() {
        if (!toBeRemoved.isEmpty()) {
            for (List<FallingBlock> blockGroupToBeRemoved : toBeRemoved) {
                removeBlockGroup(blockGroupToBeRemoved);
            }
            toBeRemoved = new ArrayList<>();
        }

        if (blocksGroupsThrown.isEmpty()) {
            return;
        }

        for (List<FallingBlock> blockGroup : blocksGroupsThrown) {
            for (FallingBlock block : blockGroup) {
                if (block.isOnGround()) {
                    if (!toBeRemoved.contains(blockGroup)) {
                        toBeRemoved.add(blockGroup);
                    }
                    break;
                }

                if (toBeRemoved.contains(blockGroup)) {
                    break;
                }

                World blockWorld = block.getWorld();

                Collection<? extends Entity> collidingStuff = blockWorld.getNearbyEntities(block.getBoundingBox());

                for (Entity collidingEntity : collidingStuff) {
                    if (!(collidingEntity instanceof LivingEntity hitEntity)) {
                        continue;
                    }

                    Player playerThrower = thrownBlockGroupPlayerMap.get(blockGroup);

                    if (hitEntity == playerThrower) {
                        continue;
                    }

                    if (hitEntity.isDead()) {
                        continue;
                    }

                    hitEntity.damage(0.000001, playerThrower);
                    hitEntity.setHealth(Math.max(0, hitEntity.getHealth() - 6));

                    if (!toBeRemoved.contains(blockGroup)) {
                        toBeRemoved.add(blockGroup);
                    }
                }
            }

        }
    }

    private static void shieldDurability(Player player) {
        PlayerInventory inventory = player.getInventory();

        ItemStack offHandItem = inventory.getItemInOffHand();

        if (!offHandItem.getType().equals(Material.SHIELD)) {
            return;
        }

        if (!offHandItem.hasItemMeta()) {
            return;
        }

        Damageable damageableMeta = (Damageable) offHandItem.getItemMeta();
        assert damageableMeta != null;

        if (!damageableMeta.hasDamage()) {
            return;
        }

        damageableMeta.setDamage(0);
        offHandItem.setItemMeta(damageableMeta);
    }

    private static void effectBonus(Player player) {
        player.addPotionEffect(earthHasteEffect);
    }

    @Override
    public void passiveAction(Player player, ItemStack item) {
        shieldDurability(player);
        effectBonus(player);
    }

}
