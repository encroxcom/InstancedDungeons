package com.encrox.instanceddungeons;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.encrox.instancedregions.InstancedProtectedCuboidRegion;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class DungeonWorld {
	
	private Plugin plugin;
	private World world;
	private ArrayList<ProtectedRegion> regions;
	private int lastX;
	
	public DungeonWorld(Plugin plugin, World world) {
		this.world = world;
		this.plugin = plugin;
		regions = new ArrayList<ProtectedRegion>();
		lastX = 0;
	}
	
	public InstancedProtectedCuboidRegion allocate(int width, int length) {
		BlockVector min, max;
		InstancedProtectedCuboidRegion region;
		for(int i = 0; true; i+=16) {
			if(new ProtectedCuboidRegion("current", (min = new BlockVector(lastX+i, 0, 0)), (max = new BlockVector(lastX+i+width, 255, length))).getIntersectingRegions(regions).isEmpty()) {
				region = new InstancedProtectedCuboidRegion(plugin, world, ""+min.getBlockX(), min, max);
				regions.add(region);
				return region;
			}
		}
	}
	
	//YOU STILL HAVE TO DISPOSE THE INSTANCED REGION MANUALLY!
	public void deallocate(InstancedProtectedCuboidRegion region) {
		BlockVector min = region.getMinimumPoint(), max = region.getMaximumPoint();
		for(int y = min.getBlockY(), ymax = max.getBlockY(); y<ymax; y++) {
			for(int z = min.getBlockZ(), zmax = max.getBlockZ(); z<zmax; z++) {
				for(int x = min.getBlockX(), xmax = max.getBlockX(); x<xmax; x++) {
					world.getBlockAt(x, y, z).setType(Material.AIR);
				}
			}
		}
		regions.remove(region);
	}
	
	public World getWorld() {
		return world;
	}

}
