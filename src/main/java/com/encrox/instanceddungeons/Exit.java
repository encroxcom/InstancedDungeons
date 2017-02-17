package com.encrox.instanceddungeons;

import com.sk89q.worldedit.BlockVector;

public class Exit {
	
	private Section section;
	private BlockVector relative;
	
	public Exit(Section section, BlockVector relative) {
		this.section = section;
		this.relative = relative;
	}
	
	public Section getSection() {
		return section;
	}
	
	public BlockVector getRelativeBlockVector() {
		return relative;
	}

}
