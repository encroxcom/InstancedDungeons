package com.encrox.instanceddungeons;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONObject;

public class Commander implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player)sender;
			try {
				switch(args[0]) {
				case "open":
					break;
				case "invite":
					break;
				case "join":
					break;
				case "close":
					break;
				case "random":
					Dungeon dungeon = new Dungeon(player, args[1], Integer.parseInt(args[2]));
					InstancedDungeons.dungeons.add(dungeon);
					return true;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			sender.sendMessage("no player");
		}
		return false;
	}

}
