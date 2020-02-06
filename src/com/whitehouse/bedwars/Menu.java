package com.whitehouse.bedwars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Menu {
    private final BedWars plugin;

    public Menu(BedWars plugin){
        this.plugin = plugin;
    }

    public ChatColor getColorOfNthTeam(int nth){
        if(nth==0) return ChatColor.getByChar('c');
        if(nth==1) return ChatColor.getByChar('9');
        if(nth==2) return ChatColor.getByChar('a');
        if(nth==3) return ChatColor.getByChar('e');
        if(nth==4) return ChatColor.getByChar('5');
        if(nth==5) return ChatColor.getByChar('6');
        if(nth==6) return ChatColor.getByChar('d');
        if(nth==7) return ChatColor.getByChar('3');
        return ChatColor.WHITE;
    }

    public Color getDyeColorOfNthTeam(int nth){
        if(nth==0) return Color.fromRGB(255, 0, 0);
        if(nth==1) return Color.fromRGB(50, 0, 255);
        if(nth==2) return Color.fromRGB(0, 255, 0);
        if(nth==3) return Color.fromRGB(255, 255, 0);
        if(nth==4) return Color.fromRGB(128, 0, 128);
        if(nth==5) return Color.fromRGB(255, 160, 0);
        if(nth==6) return Color.fromRGB(255, 128, 150);
        if(nth==7) return Color.fromRGB(0, 160, 160);
        return Color.WHITE;
    }

    public Material getWoolOfNthTeam(int nth){
        if(nth==0) return Material.RED_WOOL;
        if(nth==1) return Material.BLUE_WOOL;
        if(nth==2) return Material.LIME_WOOL;
        if(nth==3) return Material.YELLOW_WOOL;
        if(nth==4) return Material.PURPLE_WOOL;
        if(nth==5) return Material.ORANGE_WOOL;
        if(nth==6) return Material.PINK_WOOL;
        if(nth==7) return Material.CYAN_WOOL;
        return Material.WHITE_WOOL;
    }

    public String getNameOfNthTeam(int nth){
        if(nth==0) return "§cCerveny tym";
        if(nth==1) return "§9Modry tym";
        if(nth==2) return "§aZeleny tym";
        if(nth==3) return "§eZluty tym";
        if(nth==4) return "§5Fialovy tym";
        if(nth==5) return "§6Oranzovy tym";
        if(nth==6) return "§dRuzovy tym";
        if(nth==7) return "§3Tyrkysovy tym";
        return "Bezbarvy tym";
    }

    public void openTeamSelectMenu(Player player){
        Inventory inv = Bukkit.createInventory(null, 9, this.plugin.getConfig().getString("main.teamSelectMenuName"));
        ItemMeta im = null;
        //Vytvorit jednotlive itemy pro tymy
        int teamCount = this.plugin.getConfig().getInt("arena.teams");
        int playersPerTeam = this.plugin.getConfig().getInt("arena.playersPerTeam");
        for(int i=0; i<teamCount; i++){
            ItemStack wool = new ItemStack(getWoolOfNthTeam(i));
            im = wool.getItemMeta();
            ArrayList<Player> playersInTeam = this.plugin.getPlayersInTeam(i);
            int playersInTeamCount = playersInTeam.size();
            im.setDisplayName(getNameOfNthTeam(i)+" "+playersInTeamCount+"/"+playersPerTeam);
            ArrayList<String> lore = new ArrayList<String>();
            for(Player p : playersInTeam){
                lore.add("§f- "+p.getName());
            }
            if(playersInTeamCount < playersPerTeam) lore.add(plugin.getConfig().getString("main.teamClickToJoin"));
            else lore.add(plugin.getConfig().getString("main.teamIsFull"));
            im.setLore(lore);
            wool.setItemMeta(im);
            inv.setItem(i, wool);
        }
        player.openInventory(inv);
    }

    public String getItemPriceString(String configKey){
        String priceFromConfig = this.plugin.getConfig().getString(configKey);
        try {
            String[] split = priceFromConfig.split(",");
            if (split[0].equalsIgnoreCase("iron")) {
                return "§7" + split[1] + " IRON";
            } else if (split[0].equalsIgnoreCase("gold")) {
                return "§6" + split[1] + " GOLD";
            } else if (split[0].equalsIgnoreCase("diamond")) {
                return "§b" + split[1] + " DIAMOND";
            } else if (split[0].equalsIgnoreCase("emerald")) {
                return "§2" + split[1] + " EMERALD";
            }
            return split[1] + " " + split[0].toUpperCase();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public ArrayList<ItemStack> getShopCategoryListItems(){
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        ItemMeta im = null;

        ItemStack cat_1 = new ItemStack(Material.DIAMOND_CHESTPLATE);
        im = cat_1.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categoryArmor"));
        if(im!=null) im.setLore(Collections.singletonList("§7Na cely zapas"));
        cat_1.setItemMeta(im);
        list.add(cat_1);

        ItemStack cat_2 = new ItemStack(Material.DIAMOND_SWORD);
        im = cat_2.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categoryWeapons"));
        cat_2.setItemMeta(im);
        list.add(cat_2);

        ItemStack cat_3 = new ItemStack(Material.BOW);
        im = cat_3.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categoryBows"));
        cat_3.setItemMeta(im);
        list.add(cat_3);

        ItemStack cat_4 = new ItemStack(Material.SANDSTONE);
        im = cat_4.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categoryBlocks"));
        cat_4.setItemMeta(im);
        list.add(cat_4);

        ItemStack cat_5 = new ItemStack(Material.STONE_PICKAXE);
        im = cat_5.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categoryTools"));
        cat_5.setItemMeta(im);
        list.add(cat_5);

        ItemStack cat_6 = new ItemStack(Material.TNT);
        im = cat_6.getItemMeta();
        if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.categorySpecial"));
        cat_6.setItemMeta(im);
        list.add(cat_6);

        return list;
    }

    public ArrayList<ItemStack> getShopCategoryItems(int cat, Player player){
        ArrayList<ItemStack> list = new ArrayList<ItemStack>();
        ItemMeta im = null;

        if(cat == 0){
            //Armor

            ItemStack item_1 = new ItemStack(Material.LEATHER_CHESTPLATE);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.armor.leather"));
            if(im!=null) im.setLore(Arrays.asList("§7Na cely zapas", "§fHelma, kalhoty a boty", getItemPriceString("shop.prices.armor.leather")));
            item_1.setItemMeta(im);
            list.add(item_1);

            ItemStack item_2 = new ItemStack(Material.IRON_CHESTPLATE);
            im = item_2.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.armor.iron"));
            if(im!=null) im.setLore(Arrays.asList("§7Na cely zapas", "§fHelma, kalhoty a boty", getItemPriceString("shop.prices.armor.iron")));
            item_2.setItemMeta(im);
            list.add(item_2);

            ItemStack item_3 = new ItemStack(Material.DIAMOND_CHESTPLATE);
            im = item_3.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.armor.diamond"));
            if(im!=null) im.setLore(Arrays.asList("§7Na cely zapas", "§fHelma, kalhoty a boty", getItemPriceString("shop.prices.armor.diamond")));
            item_3.setItemMeta(im);
            list.add(item_3);

        }
        else if(cat == 1){
            //Weapons

            ItemStack item_1 = new ItemStack(Material.STICK);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.weapons.stick"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.weapons.stick")));
            item_1.setItemMeta(im);
            item_1.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
            list.add(item_1);

            ItemStack item_2 = new ItemStack(Material.GOLDEN_SWORD);
            im = item_2.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.weapons.sword1"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.weapons.sword1")));
            item_2.setItemMeta(im);
            item_2.addEnchantment(Enchantment.DURABILITY, 3);
            item_2.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            list.add(item_2);

            ItemStack item_3 = new ItemStack(Material.GOLDEN_SWORD);
            im = item_3.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.weapons.sword2"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.weapons.sword2")));
            item_3.setItemMeta(im);
            item_3.addEnchantment(Enchantment.DURABILITY, 3);
            item_3.addEnchantment(Enchantment.DAMAGE_ALL, 3);
            list.add(item_3);

        }
        else if(cat == 2){
            //Bows

            ItemStack item_1 = new ItemStack(Material.BOW);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.bows.bow1"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.bows.bow1")));
            item_1.setItemMeta(im);
            list.add(item_1);

            ItemStack item_2 = new ItemStack(Material.BOW);
            im = item_2.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.bows.bow2"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.bows.bow2")));
            item_2.setItemMeta(im);
            item_2.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
            list.add(item_2);

            ItemStack item_3 = new ItemStack(Material.ARROW);
            item_3.setAmount(4);
            im = item_3.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.bows.four_arrows"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.bows.four_arrows")));
            item_3.setItemMeta(im);
            list.add(item_3);

        }
        else if(cat == 3){
            //Blocks

            ItemStack item_1 = new ItemStack(getWoolOfNthTeam(this.plugin.getTeamOfPlayer(player)));
            item_1.setAmount(8);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.blocks.eight_wool"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.blocks.eight_wool")));
            item_1.setItemMeta(im);
            list.add(item_1);

            ItemStack item_2 = new ItemStack(Material.OAK_PLANKS);
            item_2.setAmount(8);
            im = item_2.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.blocks.eight_planks"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.blocks.eight_planks")));
            item_2.setItemMeta(im);
            list.add(item_2);

            ItemStack item_3 = new ItemStack(Material.END_STONE);
            item_3.setAmount(4);
            im = item_3.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.blocks.four_endstone"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.blocks.four_endstone")));
            item_3.setItemMeta(im);
            list.add(item_3);

            ItemStack item_4 = new ItemStack(Material.OBSIDIAN);
            im = item_4.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.blocks.obsidian"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.blocks.obsidian")));
            item_4.setItemMeta(im);
            list.add(item_4);

        }
        else if(cat == 4){
            //Tools

            ItemStack item_1 = new ItemStack(Material.STONE_PICKAXE);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.tools.stone_pickaxe"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.tools.stone_pickaxe")));
            item_1.setItemMeta(im);
            list.add(item_1);

            ItemStack item_2 = new ItemStack(Material.DIAMOND_PICKAXE);
            im = item_2.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.tools.diamond_pickaxe"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.tools.diamond_pickaxe")));
            item_2.setItemMeta(im);
            list.add(item_2);

            ItemStack item_3 = new ItemStack(Material.SHEARS);
            im = item_3.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.tools.shears"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.tools.shears")));
            item_3.setItemMeta(im);
            list.add(item_3);

        }
        else if(cat == 5){
            //Special

            ItemStack item_1 = new ItemStack(Material.FIRE_CHARGE);
            im = item_1.getItemMeta();
            if(im!=null) im.setDisplayName(this.plugin.getConfig().getString("shop.names.special.fireball"));
            if(im!=null) im.setLore(Collections.singletonList(getItemPriceString("shop.prices.special.fireball")));
            item_1.setItemMeta(im);
            list.add(item_1);

        }

        return list;
    }

    public void openShopMenu(Player player){
        Inventory inv = Bukkit.createInventory(null, 45, this.plugin.getConfig().getString("game.shopMenuName"));
        //V prvnim radku jsou kategorie polozek
        ArrayList<ItemStack> list = this.getShopCategoryListItems();
        int i=0;
        for(ItemStack is : list) {
            inv.setItem(i, is);
            ++i;
        }

        player.openInventory(inv);
    }
}
