package com.encrox.instanceddungeons;

import java.io.File;
import java.io.IOException;
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
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.MCEditSchematicFormat;

public class Section {
	
	private Schematic schematic;
	private String schematicFileName;
	private Map<BlockVector,Exit> exitMap;
	private ArrayList<Player> players;
	private BlockVector[] exits;
	private BlockVector size, nearestPositiveX, nearestNegativeX, nearestPositiveY, nearestNegativeY, nearestPositiveZ, nearestNegativeZ;
	private int index, depth;
	private InstancedProtectedCuboidRegion region;
	private Listener trigger;
	private volatile boolean justEntered, portal = false;
	private JSONObject descriptor;
	
	public Section(JSONObject descriptor, int index, int depth) {
		this.index = index;
		this.depth = depth;
		players = new ArrayList<Player>();
		exitMap = new HashMap<BlockVector,Exit>();
		schematicFileName = descriptor.getString("file");
		JSONArray size = descriptor.getJSONArray("size");
		this.size = new BlockVector(size.getInt(0), size.getInt(1), size.getInt(2));
		JSONArray exits = descriptor.getJSONArray("exits");
		this.exits = new BlockVector[exits.length()];
		for(int i = 0; i<this.exits.length; i++) {
			JSONArray current = exits.getJSONArray(i);
			this.exits[i] = new BlockVector(current.getInt(0), current.getInt(1), current.getInt(2));
		}
		this.descriptor = descriptor;
	}
	
	public void load() throws Exception {
		this.schematic = new Schematic(new File(InstancedDungeons.schematicsDirectory, schematicFileName));
		if(index <= depth) {
			Section next;
			for(int i = 0; i<exits.length; i++) {
				ArrayList<Exit> usable = new ArrayList<Exit>();
				if(exits[i].getBlockX() == 0.0 || exits[i].getBlockX() == size.getBlockX()-1 || exits[i].getBlockZ() == 0.0 || exits[i].getBlockZ() == size.getBlockZ()-1) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasSideExit()) {
								usable.addAll(next.getSideExits());
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasSideExit()) {
								usable.addAll(next.getSideExits());
							}
						}
					}
				} else if(exits[i].getBlockY() == 0.0) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasTopExit()) {
								usable.addAll(next.getTopExits());
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasTopExit()) {
								usable.addAll(next.getTopExits());
							}
						}
					}
				} else if(exits[i].getBlockY() == size.getBlockY()-1) {
					if(index == depth) {
						for(int c = 0, length = InstancedDungeons.endDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.endDescriptors.getJSONObject(c), index+1, depth)).hasBottomExit()) {
								usable.addAll(next.getBottomExits());
							}
						}
					} else {
						for(int c = 0, length = InstancedDungeons.normalDescriptors.length(); c<length; c++) {
							if((next = new Section(InstancedDungeons.normalDescriptors.getJSONObject(c), index+1, depth)).hasBottomExit()) {
								usable.addAll(next.getBottomExits());
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
	
	public BlockVector[] getExits() {
		return exits;
	}
	
	public BlockVector[] getAbsoluteExits() {
		BlockVector[] out = new BlockVector[exits.length];
		BlockVector min = region.getMinimumPoint();
		for(int i = 0; i<exits.length; i++) {
			out[i] = new BlockVector(min.getBlockX()+exits[i].getBlockX(), 128+exits[i].getBlockY(), min.getBlockZ()+exits[i].getBlockZ());
		}
		return out;
	}
	
	public BlockVector absoluteToRelativeExit(BlockVector bv) {
		BlockVector min = region.getMinimumPoint();
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == bv.getBlockX()-min.getBlockX()
					&& exits[i].getBlockY() == bv.getBlockY()-128
					&& exits[i].getBlockZ() == bv.getBlockZ()-min.getBlockZ())
				return exits[i];
		}
		return null;
	}
	
	public BlockVector relativeToAbsoluteExit(BlockVector bv) {
		BlockVector min = region.getMinimumPoint();
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == bv.getBlockX()
					&& exits[i].getBlockY() == bv.getBlockY()
					&& exits[i].getBlockZ() == bv.getBlockZ())
				return new BlockVector(min.getBlockX()+exits[i].getBlockX(), 128+exits[i].getBlockY(), min.getBlockZ()+exits[i].getBlockZ());
		}
		return null;
	}
	
	public ArrayList<Exit> getSideExits() {
		ArrayList<Exit> out = new ArrayList<Exit>();
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == 0.0 || exits[i].getBlockX() == size.getBlockX()-1.0
					|| exits[i].getBlockZ() == .0 || exits[i].getBlockZ() == size.getBlockZ()-1.0) {
				out.add(new Exit(this, exits[i]));
			}
		}
		return out;
	}
	
	public ArrayList<Exit> getTopExits() {
		ArrayList<Exit> out = new ArrayList<Exit>();
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == size.getBlockY()-1.0) {
				out.add(new Exit(this, exits[i]));
			}
		}
		return out;
	}
	
	public ArrayList<Exit> getBottomExits() {
		ArrayList<Exit> out = new ArrayList<Exit>();
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == 0.0) {
				out.add(new Exit(this, exits[i]));;
			}
		}
		return out;
	}
	
	public BlockVector getNearestExitPositiveX(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == size.getBlockX()-1) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public BlockVector getNearestExitNegativeX(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockX() == 0.0) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public BlockVector getNearestExitPositiveY(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == size.getBlockY()-1) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public BlockVector getNearestExitNegativeY(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockY() == 0.0) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public BlockVector getNearestExitPositiveZ(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockZ() == size.getBlockZ()-1) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public BlockVector getNearestExitNegativeZ(org.bukkit.util.Vector relative) {
		BlockVector nearest = null;
		double dist = 0.0, current;
		for(int i = 0; i<exits.length; i++) {
			if(exits[i].getBlockZ() == 0.0) {
				if((current = exits[i].distance(new Vector(relative.getX(), relative.getY(), relative.getZ()))) > dist) {
					dist = current;
					nearest = exits[i];
				}
			}
		}
		return nearest;
	}
	
	public void instantiate() {
		InstancedDungeons.logger.info("Instantiating section (file: " + descriptor.getString("file") + ", index: " + index + ", depth: " + depth + ")");
		justEntered = true;
		portal = false;
		region = InstancedDungeons.dungeonWorld.allocate(size.getBlockX(), size.getBlockZ());
		Iterator<Player> iter = players.iterator();
		while(iter.hasNext())
			region.addPlayer(iter.next());
		//write schematic to map
		final BlockVector min = region.getMinimumPoint();
		
		for(int i = 0; i<exits.length; i++) {
			try {
				exitMap.get(exits[i]).getSection().load();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//DEBUG
		for(int i = 0; i<exits.length; i++) {
			InstancedDungeons.logger.info("Exit at: (" + (min.getBlockX()+exits[i].getBlockX()) + ", " + (128+exits[i].getBlockY()) + ", " + (min.getBlockZ()+exits[i].getBlockZ()));
		}
		
		for(int y = 0; y<schematic.height; y++) {
			for(int z = 0; z<schematic.length; z++) {
				for(int x = 0; x<schematic.width; x++) {
					region.addToChangeWhitelist(new BlockVector(min.getBlockX()+x, 128+y, min.getBlockZ()+z));
				}
			}
		}
		
		EditSession es = InstancedDungeons.we.getWorldEdit().getEditSessionFactory().getEditSession(new BukkitWorld(InstancedDungeons.dungeonWorld.getWorld()), Integer.MAX_VALUE);
		try {
			MCEditSchematicFormat.getFormat(schematic.file).load(schematic.file).paste(es, new Vector(min.getBlockX()-1, 128, min.getBlockZ()), false);
		} catch (MaxChangedBlocksException | DataException | IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		byte next;
		for(int y = 0; y<schematic.height; y++) {
			for(int z = 0; z<schematic.length; z++) {
				for(int x = 0; x<schematic.width; x++) {
					next = schematic.getNextBlock();
					region.addToChangeWhitelist(new BlockVector(min.getBlockX()+x, 128+y, min.getBlockZ()+z));
					InstancedDungeons.dungeonWorld.getWorld().getBlockAt(min.getBlockX()+x, 128+y, min.getBlockZ()+z).setType(Material.getMaterial(next));
					
				}
			}
		}
		*/
		Bukkit.getScheduler().scheduleSyncDelayedTask(InstancedDungeons.plugin, new Runnable() {
			@Override
			public void run() {
				InstancedDungeons.logger.info("portal activated");
				justEntered = false;
			}
		}, 120L);
		trigger = new Listener() {
			@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
			public void onMove(PlayerMoveEvent event) {
				Location to = event.getTo();
				Player player = event.getPlayer();
				org.bukkit.util.Vector tov = to.toVector();
				BlockVector nearest;
				//draw x+ if changed
				if((nearest = getNearestExitPositiveX(tov)) != null) {
					if(nearest != nearestPositiveX) {
						nearestPositiveX = nearest;
						BlockVector exit = exitMap.get(nearest).getRelativeBlockVector(), absolute = relativeToAbsoluteExit(nearest);
						Section section = exitMap.get(nearest).getSection();
						Schematic schematic = section.getSchematic();
						if(exit.getBlockX() == 0.0) {
							//rotate along y axis by 0 degree
							InstancedDungeons.logger.info("negative x");
							for(int y = 0; y<schematic.height; y++) {
								for(int z = 0; z<schematic.length; z++) {
									for(int x = 0; x<schematic.width; x++) {
										region.addToChangeWhitelist(new BlockVector(absolute.getBlockX()+1+x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockZ()+z));
										player.sendBlockChange(new Location(InstancedDungeons.dungeonWorld.getWorld(), absolute.getBlockX()+1+x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockZ()+z), Material.getMaterial(schematic.getNextBlock()), (byte) 0);
									}
								}
							}
						} else if(exit.getBlockX() == section.size.getBlockX()-1) { //works
							InstancedDungeons.logger.info("positive x");
							for(int y = 0; y<schematic.height; y++) {
								for(int z = 0; z<schematic.length; z++) {
									for(int x = 0; x<schematic.width; x++) {
										region.addToChangeWhitelist(new BlockVector(absolute.getBlockX()+schematic.width-x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockZ()+z));
										player.sendBlockChange(new Location(InstancedDungeons.dungeonWorld.getWorld(), absolute.getBlockX()+schematic.width-x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockZ()+z), Material.getMaterial(schematic.getNextBlock()), (byte) 0);
									}
								}
							}
						} else if(exit.getBlockZ() == 0.0) {
							InstancedDungeons.logger.info("negative z");
							for(int y = 0; y<schematic.height; y++) {
								for(int z = 0; z<schematic.length; z++) {
									for(int x = 0; x<schematic.width; x++) {
										region.addToChangeWhitelist(new BlockVector(absolute.getBlockX()+1+x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()+1+z));
										player.sendBlockChange(new Location(InstancedDungeons.dungeonWorld.getWorld(), absolute.getBlockX()+1+x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()+1+z), Material.getMaterial(schematic.getNextBlock()), (byte) 0);
									}
								}
							}
						} else if(exit.getBlockZ() == section.size.getBlockZ()-1) {
							InstancedDungeons.logger.info("positive z");
							for(int y = 0; y<schematic.height; y++) {
								for(int z = 0; z<schematic.length; z++) {
									for(int x = 0; x<schematic.width; x++) {
										region.addToChangeWhitelist(new BlockVector(absolute.getBlockX()+schematic.length-x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockX()+z));
										player.sendBlockChange(new Location(InstancedDungeons.dungeonWorld.getWorld(), absolute.getBlockX()+schematic.length-x, absolute.getBlockY()-exit.getBlockY()+y, absolute.getBlockZ()-exit.getBlockX()+z), Material.getMaterial(schematic.getNextBlock()), (byte) 0);
									}
								}
							}
						}
					}
				}
				if(!justEntered) {
					if(portal) {
						for(int i = 0; i<exits.length; i++) {
							if(to.getWorld().equals(InstancedDungeons.dungeonWorld.getWorld())
									&& to.getBlockX() == min.getBlockX() + exits[i].getBlockX()
									&& to.getBlockY() == 128 + exits[i].getBlockY()
									&& to.getBlockZ() == min.getBlockZ() + exits[i].getBlockZ()) {
								InstancedDungeons.logger.info("Player in loading zone");
								portal = false;
								Section next = exitMap.get(exits[i]).getSection();
								try {
									region.dispose();
									for(int c = 0, size = players.size(); c<size; c++) {
										next.addPlayer(players.get(c));
									}
									next.instantiate();
									BlockVector destination = next.relativeToAbsoluteExit(exitMap.get(exits[i]).getRelativeBlockVector());
									//next.assignExit(destination, new Section(descriptor, index, depth));
									event.getPlayer().teleport(new Location(Bukkit.getWorld("world_nether"), -100000, 200, -100000));
									event.getPlayer().teleport(new Location(InstancedDungeons.dungeonWorld.getWorld(), destination.getBlockX(), destination.getBlockY(), destination.getBlockZ()).add(.5, 0, .5));
									InstancedDungeons.dungeonWorld.deallocate(region);
									HandlerList.unregisterAll(this);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					} else {
						for(int i = 0; i<exits.length; i++) {
							if(to.getWorld().equals(InstancedDungeons.dungeonWorld.getWorld())
									&& to.getBlockX() == min.getBlockX() + exits[i].getBlockX()
									&& to.getBlockY() == 128 + exits[i].getBlockY()
									&& to.getBlockZ() == min.getBlockZ() + exits[i].getBlockZ()) {
								return;
							}
						}
						portal = true;
					}
				}
			}
		};
		Bukkit.getPluginManager().registerEvents(trigger, InstancedDungeons.plugin);
		region.apply();
	}
	
	public Schematic getSchematic() {
		return schematic;
	}
	
	public void activateExits() {
		
	}
	
	public void assignExit(BlockVector v, Exit exit) {
		exitMap.put(v, exit);
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
			Iterator<Exit> exits = exitMap.values().iterator();
			while(exits.hasNext())
				exits.next().getSection().addPlayer(player);
		}
	}
	
	public void removePlayer(Player player) {
		players.add(player);
		if(region != null)
			region.removePlayer(player);
		if(exitMap != null) {
			Iterator<Exit> exits = exitMap.values().iterator();
			while(exits.hasNext())
				exits.next().getSection().removePlayer(player);
		}
	}

}
