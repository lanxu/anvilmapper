package anvilmapper.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import mapwriter.region.Nbt;
import mapwriter.region.RegionManager;

public class IdMaps
{
	private static final Pattern PATTERN_ID_OVERRIDES_BIOMES = Pattern.compile("^([0-9]+)\\s*=\\s*([a-zA-Z0-9_\\+\\(\\) -]+)$");
	private static final Pattern PATTERN_ID_OVERRIDES_BLOCKS = Pattern.compile("^([0-9]+)\\s*=\\s*([a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+)$");
	private static final Map<String, Integer> VANILLA_BLOCK_ID_MAP = new HashMap<String, Integer>();
	private static final Map<String, Integer> VANILLA_BIOME_ID_MAP = new HashMap<String, Integer>();

	public enum MapType
	{
		BLOCKS,
		BIOMES;
	}

	public static Map<String, Integer> getIdMap(MapType type, File worldDir, File fileIdOverrides)
	{
		Map<String, Integer> map = getIdMapFromLevel(type, worldDir);

		if (map.isEmpty())
		{
			map.putAll(type == MapType.BLOCKS ? VANILLA_BLOCK_ID_MAP : VANILLA_BIOME_ID_MAP);
		}

		if (fileIdOverrides.isFile())
		{
			map = supplementIdMap(map, readIdOverrides(type, fileIdOverrides));
		}

		return map;
	}

	private static Map<String, Integer> getIdMapFromLevel(MapType type, File worldDir)
	{
		File levelFile = new File(worldDir, "level.dat");

		if (levelFile.isFile())
		{
			Map<String, Integer> newMap = new HashMap<String, Integer>();

			try
			{
				DataInputStream data = getDataStreamForNBTFile(levelFile);

				if (data != null)
				{
					String name = type == MapType.BLOCKS ? "minecraft:blocks" : "minecraft:biomes";
					// This will be a NBTTagList, containing compound tags, one per entry
					Nbt registry = Nbt.readNextElement(data).getChild("FML").getChild("Registries").getChild(name).getChild("ids");
					int count = registry.size();

					for (int i = 0; i < count; i++)
					{
						Nbt tag = registry.getChild(i);
						newMap.put(tag.getChild("K").getString(), tag.getChild("V").getInt());
					}
				}
				else
				{
					throw new IOException();
				}
			}
			catch (IOException e)
			{
				RegionManager.logWarning("Failed to read ID map from level file '%s'", levelFile.getPath());
			}

			return newMap;
		}

		return Collections.emptyMap();
	}

	private static Map<String, Integer> readIdOverrides(MapType type, File file)
	{
		Map<String, Integer> map = new HashMap<String, Integer>();

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null)
			{
				Pattern pattern = type == MapType.BIOMES ? PATTERN_ID_OVERRIDES_BIOMES : PATTERN_ID_OVERRIDES_BLOCKS;
				Matcher matcher = pattern.matcher(line);

				if (matcher.matches())
				{
					map.put(matcher.group(2), Integer.parseInt(matcher.group(1)));
				}
				else
				{
					RegionManager.logWarning("Invalid line in the ID map overrides file: '%s'", line);
				}
			}

			reader.close();
		}
		catch (IOException | NumberFormatException e)
		{
			RegionManager.logWarning("Failed to read ID map overrides from file '%s'", file.getPath());
		}

		return map;
	}

	private static Map<String, Integer> supplementIdMap(Map<String, Integer> original, Map<String, Integer> overrides)
	{
		Map<String, Integer> newMap = new HashMap<String, Integer>();

		newMap.putAll(original);
		newMap.putAll(overrides);

		return newMap;
	}

	private static DataInputStream getDataStreamForNBTFile(File file)
	{
		try
		{
			return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))));
		}
		catch (IOException e)
		{
			RegionManager.logWarning("Failed to read NBT from file '%s'", file.getPath());
			e.printStackTrace();
			return null;
		}
	}
}
