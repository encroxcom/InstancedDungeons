/*
 * Dynamic Schematic object
 * 
 * Currently not supporting entities.
 */

package com.encrox.instanceddungeons;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

public class Schematic {
	
	public int width, height, length, readIndex, writeIndex;
	public String materials;
	public byte[] blocks, data;
	public List<Tag> entities, tileEntities;
	
	public Schematic(int width, int height, int length, String materials, byte[] blocks, byte[] data, List<Tag> entities, List<Tag> tileEntities) {
		this.width = width;
		this.height = height;
		this.length = length;
		this.materials = materials;
		this.blocks = blocks;
		this.data = data;
		this.entities = entities;
		this.tileEntities = tileEntities;
		readIndex = -1;
		writeIndex = -1;
	}
	
	public Schematic(File file) {
		CompoundTag schematicTag = null;
		try {
			NBTInputStream in = new NBTInputStream(new FileInputStream(file));
			schematicTag = (CompoundTag)in.readTag();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String,Tag> schematic = schematicTag.getValue();
		width = ((ShortTag)schematic.get("Width")).getValue();
		height = ((ShortTag)schematic.get("Height")).getValue();
		length = ((ShortTag)schematic.get("Length")).getValue();
		materials = ((StringTag)schematic.get("Materials")).getValue();
		blocks = ((ByteArrayTag)schematic.get("Blocks")).getValue();
		data = ((ByteArrayTag)schematic.get("Data")).getValue();
		entities = ((ListTag)schematic.get("Entities")).getValue();
		tileEntities = ((ListTag)schematic.get("TileEntities")).getValue();
	}
	
	public byte getBlockIdAt(int x, int y, int z) {
		return blocks[(y*length + z)*width + x];
	}
	
	public byte getNextBlock() {
		return blocks[readIndex++];
	}
	
	public void setBlockIdAt(int x, int y, int z, byte blockId) {
		blocks[(y*length + z)*width + x] = blockId;
	}
	
	public void setNextBlock(byte blockId) {
		blocks[writeIndex++] = blockId;
	}
	
	public void write(File file) {
		
	}

}
