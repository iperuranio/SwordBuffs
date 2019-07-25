package it.evermine.swordbuffs;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.Map;

import static it.evermine.swordbuffs.SwordBuffs.addEffect;
import static it.evermine.swordbuffs.SwordBuffs.createSword;
import static it.evermine.swordbuffs.SwordBuffs.isSword;

public class AddBuff implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            sender.sendMessage("Comando sconosciuto.");
            return true;
        }

        //addbuff <player> <buff> <livello>
        //Effetti: FORZA

        Player toAdd = Bukkit.getPlayer(args[0]);
        ItemStack item = toAdd.getItemInHand();
        final double balance = Vault.getBalance(toAdd);

        if(item == null || item.getType() != Material.DIAMOND_SWORD) {
            toAdd.sendMessage("§2§lFazioni §8§l» §cItem non valido.");
            return true;
        }

        if(args[1].equals("reset")) {
            if(!SwordBuffs.isSword(item)) {
                toAdd.sendMessage("§2§lFazioni §8§l» §cQuesta spada non possiede effetti.");
                return true;
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
            return true;
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
                    return true;
                }

                replacingEffects = true;
            }
        }

        if(balance <= price) {
            toAdd.sendMessage("§2§lFazioni §8§l» §cNon hai abbastanza soldi!");
            return true;
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
        return true;
    }
}
