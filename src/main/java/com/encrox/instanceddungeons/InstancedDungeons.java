package com.encrox.instanceddungeons;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class InstancedDungeons extends JavaPlugin {
	
	public static Plugin plugin;
	public static Logger logger;
	public static PluginDescriptionFile pdf;
	//public static Properties lang;
	public static JSONObject config;
	public static JSONArray resetDungeons, schematicDescriptors, startDescriptors, endDescriptors, normalDescriptors;
	public static DungeonWorld dungeonWorld;
	public static File schematicsDirectory;
	public static ArrayList<Dungeon> dungeons;
	public static WorldEditPlugin we;
	
	public void onEnable() {
		pdf = getDescription();
		logger = Logger.getLogger("Minecraft");
		if(setupMyself() && setupWorldEdit()) {
			plugin = this;
			getCommand("dungeon").setExecutor(new Commander());
			logger.info(pdf.getName() + " " + pdf.getVersion() + " has been enabled.");
		} else {
			logger.info(pdf.getName() + " " + pdf.getVersion() + " has been disabled.");
		}
	}
	
	public boolean setupWorldEdit() {
		we = (WorldEditPlugin)Bukkit.getPluginManager().getPlugin("WorldEdit");
		return (we != null);
	}
	
	public boolean setupMyself() {
		if(!this.getDataFolder().exists())
			this.getDataFolder().mkdirs();
		schematicsDirectory = new File(this.getDataFolder(), "schematics");
		if(!schematicsDirectory.exists())
			schematicsDirectory.mkdirs();
		File configFile = new File(this.getDataFolder(), "config.json");
		if(!configFile.exists()) {
			BufferedInputStream bis;
			FileOutputStream out;
			try {
				bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("config.json"));
				out = new FileOutputStream(configFile);
				int current;
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		BufferedReader br;
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			br = new BufferedReader(new FileReader(configFile));
			while((line = br.readLine()) != null)
				sb.append(line);
			br.close();
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
		config = new JSONObject(sb.toString());
		resetDungeons = config.getJSONArray("resetDungeons");
		File schematicDescriptorFile = new File(schematicsDirectory, "descriptor.json");
		if(!schematicDescriptorFile.exists()) {
			int current;
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("descriptor.json"));
				FileOutputStream out = new FileOutputStream(schematicDescriptorFile);
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/endBottom.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "endBottom.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/endSide.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "endSide.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/endTop.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "endTop.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/startBottom.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "startBottom.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/startSide.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "startSide.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/startTop.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "startTop.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("schematics/test.schematic"));
				FileOutputStream out = new FileOutputStream(new File(schematicsDirectory, "test.schematic"));
				while((current = bis.read()) != -1) {
					out.write(current);
				}
				bis.close();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		sb = new StringBuilder();
		try {
			br = new BufferedReader(new FileReader(schematicDescriptorFile));
			while((line = br.readLine()) != null)
				sb.append(line);
			br.close();
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
		schematicDescriptors = new JSONObject(sb.toString()).getJSONArray("schematics");
		startDescriptors = new JSONArray();
		endDescriptors = new JSONArray();
		normalDescriptors = new JSONArray();
		JSONObject current;
		for(int i = 0, length = schematicDescriptors.length(); i<length; i++) {
			switch((current = schematicDescriptors.getJSONObject(i)).getInt("modifier")) {
			case 0:
				normalDescriptors.put(current);
				break;
			case 1:
				startDescriptors.put(current);
				break;
			case 2:
				endDescriptors.put(current);
				break;
			}
		}
		/*lang = new Properties();
		try {
			//TODO
			lang.load(getClass().getClassLoader().getResourceAsStream("lang/" + config.getString("locale")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		*/
		dungeonWorld = new DungeonWorld(this, Bukkit.getWorld(config.getString("world")));
		dungeons = new ArrayList<Dungeon>();
		return true;
	}
	
	public void onDisable() {
		logger.info(pdf.getName() + " " + pdf.getVersion() + " has been disabled.");
	}
	
	private void addClassPath(final URL url) throws IOException {
        final URLClassLoader sysloader = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        final Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            final Method method = sysclass.getDeclaredMethod("addURL",
                    new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { url });
        } catch (final Throwable t) {
            t.printStackTrace();
            throw new IOException("Error adding " + url
                    + " to system classloader");
        }
    }

}
