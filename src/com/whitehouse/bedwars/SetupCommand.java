package com.whitehouse.bedwars;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class SetupCommand implements CommandExecutor {
    private final BedWars plugin;

    public SetupCommand(BedWars plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("§cNutno zadavat ze hry!");
            return true;
        }
        Player player = (Player)sender;
        if(!player.hasPermission("bedwars.setup")){
            player.sendMessage("§cOmlouvam se, ale na toto nemas prava. Pokud is myslis, ze se jedna o chybu, kontaktuj administratora (bedwars.setup).");
            return true;
        }
        //Prava done
        if(args.length == 0) {
            player.sendMessage(plugin.getPrefix()+"§aSETUP: V inventari mas itemy pro nastavovani ruznych lokaci.");
            player.sendMessage("§aVlnou nastavis danou vec pro dany tym, poslednim itemem (na slotu 9) zmenis nastavovanou vec.");
            player.sendMessage("§aDale nastav §2/bw-setup teams <1-8> §apocet tymu.");
            player.sendMessage("§aDale nastav §2/bw-setup playersPerTeam <1-32> §apocet hracu v tymu.");
            player.sendMessage("§aNakonec napis §2/bw-setup done §apro uzavreni nastaveni mapy.");
            this.plugin.enableSetup();
            //Itemy do inventare
            player.getInventory().clear();
            for(int i=0; i<8; i++){
                ItemStack wool = new ItemStack(this.plugin.getMenuInstance().getWoolOfNthTeam(i));
                ItemMeta im = wool.getItemMeta();
                im.setDisplayName("Nastavit postel pro: "+this.plugin.getMenuInstance().getNameOfNthTeam(i)+" §2§lRCLICK");
                wool.setItemMeta(im);
                player.getInventory().addItem(wool);
            }
            ItemStack switcher = new ItemStack(Material.RED_BED);
            ItemMeta im = switcher.getItemMeta();
            im.setDisplayName("§f§nNastaveni postele§r §2§l RCLICK >>");
            switcher.setItemMeta(im);
            player.getInventory().addItem(switcher);
            return true;
        }else{
            if(args[0].equalsIgnoreCase("done")){
                this.plugin.disableSetup();
                this.plugin.getConfig().set("main.runSetup", false);
                this.plugin.saveConfig();
                return true;
            }
            if(args[0].equalsIgnoreCase("next")){
                ItemStack[] invCont = player.getInventory().getStorageContents();
                if(invCont[8] != null){
                    try {
                        if (invCont[8].getItemMeta().getDisplayName().contains("Nastaveni postele")) {
                            //posunout na dalsi nastaveni
                            player.getInventory().clear();
                            for(int i=0; i<8; i++){
                                ItemStack wool = new ItemStack(this.plugin.getMenuInstance().getWoolOfNthTeam(i));
                                ItemMeta im = wool.getItemMeta();
                                im.setDisplayName("Nastavit spawn pro: "+this.plugin.getMenuInstance().getNameOfNthTeam(i)+" §2§lRCLICK");
                                wool.setItemMeta(im);
                                player.getInventory().addItem(wool);
                            }
                            ItemStack switcher = new ItemStack(Material.NETHER_STAR);
                            ItemMeta im = switcher.getItemMeta();
                            im.setDisplayName("§f§nNastaveni spawnu§r §2§l RCLICK >>");
                            switcher.setItemMeta(im);
                            player.getInventory().addItem(switcher);
                        }
                        else if (invCont[8].getItemMeta().getDisplayName().contains("Nastaveni spawnu")) {
                            //posunout na dalsi nastaveni
                            ItemMeta im = null;
                            player.getInventory().clear();

                            ItemStack it1 = new ItemStack(Material.IRON_INGOT);
                            im = it1.getItemMeta();
                            im.setDisplayName("Pridat spawn na irony §2§lRCLICK");
                            it1.setItemMeta(im);
                            player.getInventory().addItem(it1);

                            ItemStack it2 = new ItemStack(Material.GOLD_INGOT);
                            im = it2.getItemMeta();
                            im.setDisplayName("Pridat spawn na goldy §2§lRCLICK");
                            it2.setItemMeta(im);
                            player.getInventory().addItem(it2);

                            ItemStack it3 = new ItemStack(Material.DIAMOND);
                            im = it3.getItemMeta();
                            im.setDisplayName("Pridat spawn na diamanty §2§lRCLICK");
                            it3.setItemMeta(im);
                            player.getInventory().addItem(it3);

                            ItemStack it4 = new ItemStack(Material.CHICKEN_SPAWN_EGG);
                            im = it4.getItemMeta();
                            im.setDisplayName("Nastavit lobby §2§lRCLICK");
                            it4.setItemMeta(im);
                            player.getInventory().addItem(it4);

                            for(int i=0; i<5; i++){
                                ItemStack span = new ItemStack(Material.BARRIER);
                                im = span.getItemMeta();
                                im.setDisplayName("(nic)§"+i);
                                span.setItemMeta(im);
                                player.getInventory().addItem(span);
                            }
                            ItemStack switcher = new ItemStack(Material.GOLD_NUGGET);
                            im = switcher.getItemMeta();
                            im.setDisplayName("§f§nOstatni nastaveni§r §2§l RCLICK >>");
                            switcher.setItemMeta(im);
                            player.getInventory().addItem(switcher);
                        }
                        else if (invCont[8].getItemMeta().getDisplayName().contains("Ostatni nastaveni")) {
                            //posunout na dalsi nastaveni
                            player.getInventory().clear();
                            for(int i=0; i<8; i++){
                                ItemStack wool = new ItemStack(this.plugin.getMenuInstance().getWoolOfNthTeam(i));
                                ItemMeta im = wool.getItemMeta();
                                im.setDisplayName("Nastavit postel pro: "+this.plugin.getMenuInstance().getNameOfNthTeam(i)+" §2§lRCLICK");
                                wool.setItemMeta(im);
                                player.getInventory().addItem(wool);
                            }
                            ItemStack switcher = new ItemStack(Material.RED_BED);
                            ItemMeta im = switcher.getItemMeta();
                            im.setDisplayName("§f§nNastaveni postele§r §2§l RCLICK >>");
                            switcher.setItemMeta(im);
                            player.getInventory().addItem(switcher);
                        }
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }
                return true;
            }
        }
        return false;
    }
}
