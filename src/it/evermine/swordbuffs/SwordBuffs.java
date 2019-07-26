package it.evermine.swordbuffs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SwordBuffs extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getLogger().log(Level.INFO, "Searching for Vault...");
        Vault.setupChat();
        if (!Vault.setupEconomy())
        {
            getServer().getLogger().log(Level.SEVERE, "Vault not found, disabling...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler
    public void drop(PlayerDropItemEvent e) {
        if(e.isCancelled())
            return;

        if(e.getItemDrop() != null && isSword(e.getItemDrop().getItemStack())) {
            removeEffectsToPlayer(e.getPlayer(), getFastTypes(e.getItemDrop().getItemStack().getItemMeta().getLore()));
        }
    }

    @EventHandler
    public void inventory(InventoryClickEvent e) {
        if(e.isCancelled()) {
            return;
        }

        if(isSword(e.getCurrentItem())) {
            removeEffectsToPlayer((Player) e.getWhoClicked(), getFastTypes(e.getCurrentItem().getItemMeta().getLore()));
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Inventory inv = e.getPlayer().getInventory();
        ItemStack newer = inv.getItem(e.getNewSlot());
        ItemStack old = inv.getItem(e.getPreviousSlot());

        int hasSword = isSword(newer) ? 1 : (isSword(old) ? 0 : -1);

        if(hasSword != -1) {
            final Player p = e.getPlayer();
            if(hasSword == 1) {
                if(inv.contains(Material.DIAMOND_SWORD, 2)) {
                    p.sendMessage("§2§lFazioni §8§l» §cPuoi avere una sola spada di diamante nell'inventario!");
                    p.playSound(p.getLocation(), Sound.ANVIL_LAND, 5, 0);
                    removeEffectsToPlayer(p, getFastTypes(newer.getItemMeta().getLore()));
                    return;
                }
                sendEffectsToPlayer(p, getFastEffects(newer.getItemMeta().getLore()));
            }
            else
                removeEffectsToPlayer(p, getFastTypes(old.getItemMeta().getLore()));

            p.playSound(e.getPlayer().getLocation(), Sound.NOTE_BASS, 5, hasSword == 1 ? -5 : 5);

        }
    }

    public static void removeEffectsToPlayer(Player p, ArrayList<PotionEffectType> fastEffects) {
        for(PotionEffectType pe : fastEffects) {
            p.removePotionEffect(pe);
        }
    }

    public static boolean isSword(ItemStack is) {
        return is != null && is.getType() == Material.DIAMOND_SWORD && is.getItemMeta() != null && is.getItemMeta().getDisplayName() != null && is.getItemMeta().getDisplayName().contains("§8[§cPowered§8]");
    }

//    public ArrayList<PotionEffect> getPotions(ArrayList<String> lore) {
//        ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
//
//        effects.addAll(p.getActivePotionEffects());
//        return effects;
//    }

    private ItemStack createSword(String name, PotionEffect starter) {
        ItemStack is = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName("§8[§cPowered§8] §7"+name+"'s Sword");

        List<String> lore = new ArrayList<>();
        lore.add("§r");
        lore.add("§7Effects:");
        lore.add("§8- §f"+starter.getType().getName()+" "+toRoman(starter.getAmplifier()));

        im.setLore(lore);
        is.setItemMeta(im);

        return is;
    }

    public static void sendEffectsToPlayer(Player p, ArrayList<PotionEffect> types) {
        for(PotionEffect po : types) {
            p.addPotionEffect(po, true);
        }
    }

    private void addBuff(@NotNull String[] args) {
        Player toAdd = Bukkit.getPlayer(args[0]);
        ItemStack item = toAdd.getItemInHand();
        final double balance = Vault.getBalance(toAdd);

        if(item == null || item.getType() != Material.DIAMOND_SWORD) {
            toAdd.sendMessage("§2§lFazioni §8§l» §cItem non valido.");
            return;
        }

        if(args[1].equals("reset")) {
            if(!SwordBuffs.isSword(item)) {
                toAdd.sendMessage("§2§lFazioni §8§l» §cQuesta spada non possiede effetti.");
                return;
            }
            final long price = Long.parseLong(args[2]);
            if(balance >= price) {
                SwordBuffs.removeEffectsToPlayer(toAdd, SwordBuffs.getFastTypes(item.getItemMeta().getLore()));
                Vault.pay(toAdd, price);
                toAdd.sendMessage("§c$"+price+" prelevati dal tuo conto.");
                toAdd.sendMessage("§2§lFazioni §8§l» §7Spada resettata con successo!");

                ItemStack newItem = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta im = newItem.getItemMeta();

                for(Map.Entry<Enchantment, Integer> entry : item.getItemMeta ().getEnchants().entrySet()) {
                    im.addEnchant(entry.getKey(), entry.getValue(), true);
                }

                item.setItemMeta(im);
                toAdd.updateInventory();
            } else {
                toAdd.sendMessage("§2§lFazioni §8§l» §cNon hai abbastanza soldi!");
            }
            return;
        }

        PotionEffectType potionType = PotionEffectType.getByName(args[1]);
        int level = Integer.parseInt(args[2])+1;
        PotionEffect po = new PotionEffect(potionType, 1, level);
        final long price = Long.parseLong(args[3]);
        boolean replacingEffects = false;

        if(isSword(item)) {
            HashMap<PotionEffectType, Integer> potions = SwordBuffs.getEffects(item);

            if(potions.containsKey(potionType)) {
                if(potions.get(potionType) >= level-1) {
                    toAdd.sendMessage("§2§lFazioni §8§l» §cPuoi solo aggiungere effetti più potenti di quelli posseduti!");
                    return;
                }

                replacingEffects = true;
            }
        }

        if(balance <= price) {
            toAdd.sendMessage("§2§lFazioni §8§l» §cNon hai abbastanza soldi!");
            return;
        }

        if(replacingEffects) {
            SwordBuffs.removeEffect(item, potionType);
        }

        if(!SwordBuffs.isSword(item)) {
            createSword(toAdd.getName(), item, po);
        } else {
            addEffect(item, po);
        }

        toAdd.updateInventory();

        SwordBuffs.removeEffectsToPlayer(toAdd, SwordBuffs.getFastTypes(item.getItemMeta().getLore()));
        SwordBuffs.sendEffectsToPlayer(toAdd, SwordBuffs.getFastEffects(item.getItemMeta().getLore()));

        Vault.pay(toAdd, price);
        toAdd.sendMessage("§c$"+price+" prelevati dal tuo conto.");
        toAdd.sendMessage("§2§lFazioni §8§l» §aBuff aggiunto con successo!");
        toAdd.playSound(toAdd.getLocation(), Sound.NOTE_PLING, 5, 0);
        return;
    }

    public static ArrayList<PotionEffectType> getFastTypes(List<String> lore) {
        ArrayList<PotionEffectType> effects = new ArrayList<>();

        if(lore == null)
            return effects;

        for(int i = 2; i < lore.size(); i++)
            effects.add(PotionEffectType.getByName(lore.get(i).split("\\s+")[1].replace("§f", "")));
        return effects;
    }

    public static ArrayList<PotionEffect> getFastEffects(List<String> lore) {
        ArrayList<PotionEffect> effects = new ArrayList<>();

        for(int i = 2; i < lore.size(); i++) {
            String[] parts = lore.get(i).split("\\s+");
            effects.add(new PotionEffect(PotionEffectType.getByName(parts[1].replace("§f", "")), Integer.MAX_VALUE, toNumber(parts[2])-1));
        }
        return effects;

    }

    public static HashMap<PotionEffectType, Integer> getEffects(ItemStack is) {
        HashMap<PotionEffectType, Integer> effects = new HashMap<>();
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());

        for(int i = 2; i < lore.size(); i++) {
            String[] parts = lore.get(i).split("\\s+");
            effects.put(PotionEffectType.getByName(parts[1].replace("§f", "")), toNumber(parts[2])-1);
        }

        return effects;
    }

    static void removeEffect(ItemStack is, PotionEffectType starter) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        List<Integer> toRemove = new ArrayList<>();

        for(int i = 2; i < lore.size(); i++) {
            String[] parts = lore.get(i).split("\\s+");
            PotionEffectType pe = PotionEffectType.getByName(parts[1].replace("§f", ""));
            if(starter.equals(pe)) {
                toRemove.add(i);
            }
        }

        int porcoddio = 0;
        for(Integer i : toRemove) {
            lore.remove(i - porcoddio);
            porcoddio++;
        }

        im.setLore(lore);
        is.setItemMeta(im);
    }

    static void addEffect(ItemStack is, PotionEffect starter) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        lore.add(2, "§8- §f"+starter.getType().getName()+" "+toRoman(starter.getAmplifier()));
        im.setLore(lore);
        is.setItemMeta(im);
    }

    static void createSword(String name, ItemStack is, PotionEffect starter) {
        ItemMeta im = is.getItemMeta();
        im.setDisplayName("§8[§cPowered§8] §7"+name+"'s Sword");

        List<String> lore = new ArrayList<>();
        lore.add("§r");
        lore.add("§7Effects:");
        lore.add("§8- §f"+starter.getType().getName()+" "+toRoman(starter.getAmplifier()));

        im.setLore(lore);
        is.setItemMeta(im);
    }

    private static Integer toNumber(String roman) {
        return roman.replace("CM", "DCD")
                .replace("M", "DD")
                .replace("CD", "CCCC")
                .replace("D", "CCCCC")
                .replace("XC", "LXL")
                .replace("C", "LL")
                .replace("XL", "XXXX")
                .replace("L", "XXXXX")
                .replace("IX", "VIV")
                .replace("X", "VV")
                .replace("IV", "IIII")
                .replace("V", "IIIII").length();
    }

    private static String toRoman(int number) {
        return String.valueOf(new char[number]).replace('\0', 'I')
                .replace("IIIII", "V")
                .replace("IIII", "IV")
                .replace("VV", "X")
                .replace("VIV", "IX")
                .replace("XXXXX", "L")
                .replace("XXXX", "XL")
                .replace("LL", "C")
                .replace("LXL", "XC")
                .replace("CCCCC", "D")
                .replace("CCCC", "CD")
                .replace("DD", "M")
                .replace("DCD", "CM");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getState() instanceof Sign) {
            Sign sign = (Sign) e.getClickedBlock().getState();

            if(sign.getLine(3).contains("§8(§f")) {
                final PotionEffect pe = new PotionEffect(PotionEffectType.getById(Integer.parseInt(sign.getLine(3).split(":")[0].replace("§8(§f", ""))), 1, Integer.parseInt(sign.getLine(3).split(":")[1].replace("§8)", "")));

                addBuff(new String[] {e.getPlayer().getName(), pe.getType().getName(), ""+pe.getAmplifier(), ""+Long.parseLong(sign.getLine(2).replace("§c$", ""))});
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "addbuff "+e.getPlayer().getName()+" "+pe.getType().getName()+" "+pe.getAmplifier()+" "+Long.parseLong(sign.getLine(2).replace("§c$", "")));
            } else if(sign.getLine(3).contains("§8[§c")) {
                final long price = Long.parseLong(sign.getLine(3).split("c")[1].replace("$", "").replace("§8]", ""));

                addBuff(new String[] {e.getPlayer().getName(), "reset", ""+price});
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        //[SwordBuffs]
        //SPEED 1
        //&b&lVelocità I
        //50000

        //Cartello finale:
        //&8&m>--------< (12 - e 2 ><)
        //Velocità I
        //&c$50000
        if(e.getPlayer().hasPermission("*") || e.getPlayer().isOp()) {
            if(e.getLine(0).equals("[SwordBuffs]")) {
                e.getPlayer().sendMessage("1) [SwordBuffs]\n2) (effetto in maiuscolo con spigot name e numero che parte da 0) SPEED 1\n3) (frase da mostrare) §b§lVelocità 1\n§r4) (prezzo intero senza simboli) 40000");
                final String separator = "§8§m>------------<";

                final PotionEffect pe = new PotionEffect(PotionEffectType.getById(Integer.parseInt(e.getLine(1).split("\\s+")[0])), 1, Integer.parseInt(e.getLine(1).split("\\s+")[1])-1);
                final long price = Long.parseLong(e.getLine(3));
                final String text = ChatColor.translateAlternateColorCodes('&', e.getLine(2));

                e.setLine(0, separator);
                e.setLine(2, "§c$"+price);
                e.setLine(3, "§8(§f"+pe.getType().getId()+":"+pe.getAmplifier()+"§8)");
                e.setLine(1, text);
            } else if(e.getLine(0).equals("[BuffsReset]")) {
                final long price = Long.parseLong(e.getLine(3));

                e.setLine(0, "§7Cliccami per");
                e.setLine(1,"§7resettare gli");
                e.setLine(2, "§7effetti attuali");
                e.setLine(3, "§8[§c$"+price+"§8]");
            }
        }
    }
}