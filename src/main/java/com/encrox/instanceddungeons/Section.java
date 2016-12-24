package com.encrox.instanceddungeons;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;

import com.encrox.instancedregions.InstancedProtectedCuboidRegion;
import com.sk89q.worldedit.BlockVector;

public class Section {
	
	private Schematic schematic;
	private String schematicFileName;
	private Map<BlockVector,Section> exitMap;
	private ArrayList<Player> players;
	private BlockVector[] exits;
	private BlockVector size;
	private int index, depth;
	private InstancedProtectedCuboidRegion region;
	private Listener moveOutOfLoadingZoneListener, moveIntoLoadingZoneListener;
	private volatile boolean justEntered, portal;
	
	public Section(JSONObject descriptor, int index, int depth) {
		this.index = index;
		this.depth = depth;
		players = new ArrayList<Player>();
		schematicFileName = descriptor.getString("file");
		JSONArray size = descriptor.getJSONArray("size");
		this.size = new BlockVector(size.getInt(0), size.getInt(1), size.getInt(2));
		JSONArray exits = descriptor.getJSONArray("exits");
		this.exits = new BlockVector[exits.length()];
		for(int i = 0; i<this.exits.length; i++) {
			JSONArray current = exits.getJSONArray(i);
			this.exits[i] = new BlockVector(current.getInt(0), current.getInt(1), current.getInt(2));
		}
	}
	
	public void load() throws Exception {
		exitMap = new HashMap<BlockVector,Section>();
		this.schematic = new Schematic(new File(InstancedDungeons.schematicsDirectory, schematicFileName));
		if(index <= depth) {
			Section next;
			ArrayList<Section> usable = new ArrayList<Section>();
			for(int i = 0; i<exits.length; i++) {
				if(exits[i].getBlockX() == 0 || exits[i].getBlockX() == size.getBlockX()-1 || exits[i].getBlockZ() == 0 || exits[i].getBlockZ() == size.getBlockZ()-1) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasSideExit()) {
								usable.add(next);
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasSideExit()) {
								usable.add(next);
							}
						}
					}
				} else if(exits[i].getBlockY() == 0) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasTopExit()) {
								usable.add(next);
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasTopExit()) {
								usable.add(next);
							}
						}
					}
				} else if(exits[i].getBlockY() == size.getBlockY()-1) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasBottomExit()) {
								usable.add(next);
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasBottomExit()) {
								usable.add(next);
							}
						}
					}
				} else {
					throw new Exception("No schematic with a valid exit found.");
				}
				exitMap.put(exits[i], usable.get((int)Math.round(Math.random()*(usable.size()-1))));
			}
		}
	}
	
	public BlockVector[] getAbsoluteExits() {
		BlockVector[] out = new BlockVector[exits.length];
		BlockVector min = region.getMinimumPoint();
		for(int i = 0; i<exits.length; i++) {
			out[i] = new BlockVector(min.getBlockX()+exits[i].getBlockX(), 128+exits[i].getBlockY(), min.getBlockZ()+exits[i].getBlockZ());
		}
		return out;
	}
	
	public void instantiate() {
		justEntered = true;
		portal = false;
		region = InstancedDungeons.dungeonWorld.allocate(size.getBlockX(), size.getBlockZ());
		//write schematic to map
		final BlockVector min = region.getMinimumPoint();
		byte next;
		for(int y = 0; y<schematic.height; y++) {
			for(int z = 0; z<schematic.length; z++) {
				for(int x = 0; x<schematic.width; x++) {
					next = schematic.getNextBlock();
					region.addToChangeWhitelist(new BlockVector(min.getBlockX()+x, 128+y, min.getBlockZ()+z));
					InstancedDungeons.dungeonWorld.getWorld().getBlockAt(min.getBlockX()+x, 128+y, min.getBlockZ()+z).setType(Material.getMaterial(next));
					region.addToChangeWhitelist(new BlockVector(min.getBlockX()+x, 128+y, min.getBlockZ()+z));
					
					for(int i = 0, size = players.size(); i<size; i++) {
						players.get(i).sendBlockChange(new Location(InstancedDungeons.dungeonWorld.getWorld(), min.getBlockX()+x, 128+y, min.getBlockZ()+z), Material.getMaterial(next), (byte) 0);
					}
					
				}
			}
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(InstancedDungeons.plugin, new Runnable() {
			@Override
			public void run() {
				InstancedDungeons.logger.info("portal activated");
				justEntered = false;
				portal = true;
			}
		}, 40L);
		moveOutOfLoadingZoneListener = new Listener() {
			@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
			public void onMove(PlayerMoveEvent event) {
				InstancedDungeons.logger.info("Player moved");
				Location to = event.getTo();
				for(int i = 0; i<exits.length; i++) {
					if((!justEntered) && (!(to.getWorld().equals(InstancedDungeons.dungeonWorld.getWorld())
							&& to.getBlockX() == min.getBlockX() + exits[i].getBlockX()
							&& to.getBlockY() == 128 + exits[i].getBlockY()
							&& to.getBlockZ() == min.getBlockZ() + exits[i].getBlockZ()))) {
						InstancedDungeons.logger.info("Player exited loading zone");
						moveIntoLoadingZoneListener = new Listener() {
							@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
							public void onMove(PlayerMoveEvent event) {
								Location to = event.getTo();
								for(int i = 0; i<exits.length; i++) {
									if(portal && (!justEntered) && (to.getWorld().equals(InstancedDungeons.dungeonWorld.getWorld())
											&& to.getBlockX() == min.getBlockX() + exits[i].getBlockX()
											&& to.getBlockY() == 128 + exits[i].getBlockY()
											&& to.getBlockZ() == min.getBlockZ() + exits[i].getBlockZ())) {
										InstancedDungeons.logger.info("Player in loading zone");
										portal = false;
										Section next = exitMap.get(exits[i]);
										try {
											HandlerList.unregisterAll(moveIntoLoadingZoneListener);
											next.load();
											next.instantiate();
											for(int c = 0, size = players.size(); c<size; c++) {
												next.addPlayer(players.get(c));
											}
											BlockVector[] destinations = next.getAbsoluteExits();
											BlockVector destination = destinations[(int)Math.round(Math.random()*destinations.length)];
											event.getPlayer().teleport(new Location(InstancedDungeons.dungeonWorld.getWorld(), -10000, 200, -10000));
											event.getPlayer().teleport(new Location(InstancedDungeons.dungeonWorld.getWorld(), destination.getBlockX(), destination.getBlockY(), destination.getBlockZ()));
											region.dispose();
											InstancedDungeons.dungeonWorld.deallocate(region);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						};
						Bukkit.getPluginManager().registerEvents(moveIntoLoadingZoneListener, InstancedDungeons.plugin);
						HandlerList.unregisterAll(moveOutOfLoadingZoneListener);
					}
				}
			}
		};
		Bukkit.getPluginManager().registerEvents(moveOutOfLoadingZoneListener, InstancedDungeons.plugin);
	}
	
	public Schematic getSchematic() {
		return schematic;
	}
	
	public void activateExits() {
		
	}
	
	public boolean hasSideExit() {
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == 0
					|| exits[i].getBlockX() == size.getBlockX()-1
					|| exits[i].getBlockZ() == 0
					|| exits[i].getBlockZ() == size.getBlockZ()-1) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasTopExit() {
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == size.getBlockY()-1) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasBottomExit() {
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == 0) {
				return true;
			}
		}
		return false;
	}
	
	public void addPlayer(Player player) {
		players.add(player);
		if(region != null)
			region.addPlayer(player);
		if(exitMap != null) {
			Iterator<Section> sections = exitMap.values().iterator();
			while(sections.hasNext())
				sections.next().addPlayer(player);
		}
	}
	
	public void removePlayer(Player player) {
		players.add(player);
		if(region != null)
			region.removePlayer(player);
		if(exitMap != null) {
			Iterator<Section> sections = exitMap.values().iterator();
			while(sections.hasNext())
				sections.next().removePlayer(player);
		}
	}

}
