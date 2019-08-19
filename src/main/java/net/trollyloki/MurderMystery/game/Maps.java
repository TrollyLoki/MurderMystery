package net.trollyloki.MurderMystery.game;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.trollyloki.MurderMystery.Main;

public class Maps {
	
	public static void startMap(String map) {
		if (Main.getPlugin().getConfig().contains("maps." + map)) {
			World world = Bukkit.getServer().getWorld(Main.getPlugin().getConfig().getString("maps." + map + ".spawn.world"));
			
			List<String> commands = Main.getConfigStringList(false, "maps." + map + ".start.commands");
			if (commands != null) {
				for (String cmd : commands) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
				}
			}
			
			ConfigurationSection blocks = Main.getPlugin().getConfig().getConfigurationSection("maps." + map + ".start.blocks");
			if (blocks != null) {
				for (String name : blocks.getKeys(false)) {
					String path = ("maps." + map + ".start.blocks." + name);
					setBlocks(path, world);
				}
			}
			
		}
	}
	
	public static void endMap(String map) {
		if (Main.getPlugin().getConfig().contains("maps." + map)) {
			World world = Bukkit.getServer().getWorld(Main.getPlugin().getConfig().getString("maps." + map + ".spawn.world"));
			
			List<String> commands = Main.getConfigStringList(false, "maps." + map + ".end.commands");
			if (commands != null) {
				for (String cmd : commands) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
				}
			}
			
			Set<String> blocks = Main.getPlugin().getConfig().getConfigurationSection("maps." + map + ".end.blocks").getKeys(false);
			if (blocks != null) {
				for (String name : blocks) {
					String path = ("maps." + map + ".end.blocks." + name);
					setBlocks(path, world);
				}
			}
			
		}
	}
	
	public static Location getBlockCoords(String path, World world) {
		try {
			String[] location = Main.getPlugin().getConfig().getString(path + ".location").split(",");
			int x = Integer.parseInt(location[0]);
			int y = Integer.parseInt(location[1]);
			int z = Integer.parseInt(location[2]);
			return new Location(world, x, y, z);
			
		} catch (NumberFormatException | NullPointerException e) {
			Main.getPlugin().getLogger().warning("Invalid coordinates for block at " + path);
		}
		
		return null;
		
	}
	
	public static void setBlocks(String path, World world) {
		
		Material material = Material.valueOf(Main.getPlugin().getConfig().getString(path + ".block"));
		Block block = world.getBlockAt(getBlockCoords(path, world));
		if (block.getState() instanceof Container) {
			Container container = (Container) block.getState();
			container.getInventory().clear();
		}
		block.setType(material);
		
		if (block.getState() instanceof Container) {
			for (String slot : Main.getPlugin().getConfig().getConfigurationSection(path + ".contents").getKeys(false)) {
				String itemPath = (path + ".contents." + slot);
				Container container = (Container) block.getState();
				container.getInventory().setItem(Integer.valueOf(slot), getItemStack(itemPath));
			}
		}
		
	}
	
	public static ItemStack getItemStack(String itemPath) {
		Material material = Material.valueOf(Main.getPlugin().getConfig().getString(itemPath + ".material"));
		int amount = Main.getPlugin().getConfig().getInt(itemPath + ".amount");
		//short dataValue = Short.parseShort(Main.getPlugin().getConfig().getString(itemPath + ".data"));
		
		ItemStack itemStack = new ItemStack(material, amount);
		ItemMeta itemMeta = itemStack.getItemMeta();
		
		itemMeta.setDisplayName(Main.getConfigString(false, itemPath + ".name"));
		itemMeta.setLore(Main.getConfigStringList(false, itemPath + ".lore"));
		if (Main.getPlugin().getConfig().getBoolean(itemPath + ".glow")) {
			itemMeta.addEnchant(Enchantment.DURABILITY, -1, true);
			itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		
		itemStack.setItemMeta(itemMeta);
		
		/*List<String> canPlaceOn = Main.getConfigStringList(false, itemPath + ".CanPlaceOn");
		List<String> canDestroy = Main.getConfigStringList(false, itemPath + ".CanDestroy");
		
		if (Main.version.equalsIgnoreCase("v1_12_R1") && (canPlaceOn != null || canDestroy != null)) {
			net.minecraft.server.v1_12_R1.ItemStack stack = CraftItemStack.asNMSCopy(itemStack);
			
			if (canPlaceOn != null) {
				NBTTagList placeTags = (NBTTagList) stack.getTag().get("CanPlaceOn");
				if (placeTags == null) placeTags = new NBTTagList();
				for (String tag : canPlaceOn) {
					placeTags.add(new NBTTagString(tag));
				}
				stack.getTag().set("CanPlaceOn", placeTags);
			}
			
			if (canDestroy != null) {
				NBTTagList destroyTags = (NBTTagList) stack.getTag().get("CanDestroy");
				if (destroyTags == null) destroyTags = new NBTTagList();
				for (String tag : canDestroy) {
					destroyTags.add(new NBTTagString(tag));
				}
				stack.getTag().set("CanDestroy", destroyTags);
			}
			
			itemStack = CraftItemStack.asCraftMirror(stack);
			
		}
		else {
			Main.getPlugin().getLogger().warning("You are running version " + Main.version +
					" CanPlaceOn and CanDestroy tags are only supported for 1.12.x");
		}*/
		
		return itemStack;
	}
	
	
	
	public static boolean createMap(String map, String name, Location loc) {
		
		if (Main.getPlugin().getConfig().getConfigurationSection("maps." + map) != null) {
			return false;
		}
		
		String location = loc.getX() + "," + loc.getY() + "," + loc.getZ();
		float yaw = loc.getYaw();
		float pitch = loc.getPitch();
		String world = loc.getWorld().getName();
		
		FileConfiguration config = Main.getPlugin().getConfig();
		config.set("maps." + map + ".name", name);
		
		config.set("maps." + map + ".spawn.location", location);
		config.set("maps." + map + ".spawn.yaw", yaw);
		config.set("maps." + map + ".spawn.pitch", pitch);
		config.set("maps." + map + ".spawn.world", world);
		
		config.set("maps." + map + ".start.commands", "[]");
		config.createSection("maps." + map + ".start.blocks");
		config.set("maps." + map + ".end.commands", "[]");
		config.createSection("maps." + map + ".end.blocks");
		
		Main.getPlugin().saveConfig();
		
		return true;
		
	}
	
	public static boolean removeMap(String map) {
		
		if (Main.getPlugin().getConfig().getConfigurationSection("maps." + map) == null) {
			return false;
		}
		
		Main.getPlugin().getConfig().set("maps." + map, null);
		Main.getPlugin().saveConfig();
		
		return true;
		
	}
	
}