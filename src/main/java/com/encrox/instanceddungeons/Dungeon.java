package com.encrox.instanceddungeons;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import com.sk89q.worldedit.BlockVector;

public class Dungeon {
	
	private Section section;
	private String id;
	private int depth;
	private ArrayList<Player> players;

	public Dungeon(Player player, String id, int depth) {
		this.id = id;
		this.depth = depth;
		section = new Section(InstancedDungeons.startDescriptors.getJSONObject((int)Math.round(Math.random()*(InstancedDungeons.startDescriptors.length()-1))), 0, depth);
		try {
			section.load();
			section.instantiate();
			BlockVector[] destinations = section.getAbsoluteExits();
			BlockVector destination = destinations[(int)Math.round(Math.random()*(destinations.length-1))];
			addPlayer(player);
			player.teleport(new Location(InstancedDungeons.dungeonWorld.getWorld(), destination.getBlockX(), destination.getBlockY(), destination.getBlockZ()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addPlayer(Player player) {
		section.addPlayer(player);
	}
	
	public void removePlayer(Player player) {
		section.removePlayer(player);
	}
	
	public String getId() {
		return id;
	}

}
