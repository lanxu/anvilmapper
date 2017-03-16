package mapwriter.region;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockColours
{
	private static final Pattern PATTERN_BLOCK_STRING_ID = Pattern.compile("([a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+)");
	private static final Pattern PATTERN_BIOME_LINE_NAME = Pattern.compile("^biome ([a-zA-Z0-9_\\+ -]+) ([a-fA-F0-9]{6}) ([a-fA-F0-9]{6}) ([a-fA-F0-9]{6})$");
	private static final Pattern PATTERN_BIOME_LINE_ID = Pattern.compile("^biome ([0-9]+) ([a-fA-F0-9]{6}) ([a-fA-F0-9]{6}) ([a-fA-F0-9]{6})$");	
	public static final int MAX_BLOCKS = 4096;
	public static final int MAX_META = 16;
	public static final int MAX_BIOMES = 256;
	private int[] bcArray = new int[MAX_BLOCKS * MAX_META];
	private int[] waterMultiplierArray = new int[MAX_BIOMES];
	private int[] grassMultiplierArray = new int[MAX_BIOMES];
	private int[] foliageMultiplierArray = new int[MAX_BIOMES];
	private BlockType[] blockTypeArray = new BlockType[MAX_BLOCKS * MAX_META];
	private final Map<String, Integer> biomeIdMap;
	private final Map<String, Integer> blockIdMap;
	
	public enum BlockType {
		NORMAL,
		GRASS,
		LEAVES,
		FOLIAGE,
		WATER,
		OPAQUE
	}

	public BlockColours(Map<String, Integer> biomeIdMap, Map<String, Integer> blockIdMap)
	{
		this.biomeIdMap = biomeIdMap;
		this.blockIdMap = blockIdMap;

		Arrays.fill(this.bcArray, 0);
		Arrays.fill(this.waterMultiplierArray, 0xffffff);
		Arrays.fill(this.grassMultiplierArray, 0xffffff);
		Arrays.fill(this.foliageMultiplierArray, 0xffffff);
		Arrays.fill(this.blockTypeArray, BlockType.NORMAL);
	}
	
	public int getColour(int blockAndMeta) {
		return this.bcArray[blockAndMeta & 0xffff];
	}
	
	public void setColour(int blockAndMeta, int colour) {
		this.bcArray[blockAndMeta & 0xffff] = colour;
	}
	
	public int getColour(int blockID, int meta) {
		return this.bcArray[((blockID & 0xfff) << 4) | (meta & 0xf)];
	}
	
	public void setColour(int blockID, int meta, int colour) {
		this.bcArray[((blockID & 0xfff) << 4) | (meta & 0xf)] = colour;
	}
	
	private int getGrassColourMultiplier(int biome) {
		return (this.grassMultiplierArray != null) && (biome >= 0) && (biome < this.grassMultiplierArray.length) ?
				this.grassMultiplierArray[biome] : 0xffffff;
	}
	
	private int getWaterColourMultiplier(int biome) {
		return (this.waterMultiplierArray != null) && (biome >= 0) && (biome < this.waterMultiplierArray.length) ?
				this.waterMultiplierArray[biome] : 0xffffff;
	}
	
	private int getFoliageColourMultiplier(int biome) {
		return (this.foliageMultiplierArray != null) && (biome >= 0) && (biome < this.foliageMultiplierArray.length) ?
				this.foliageMultiplierArray[biome] : 0xffffff;
	}
	
	public int getBiomeColour(int blockAndMeta, int biome)
	{
		blockAndMeta &= 0xffff;

		switch (this.blockTypeArray[blockAndMeta])
		{
			case GRASS:
				return getGrassColourMultiplier(biome);
			case LEAVES:
			case FOLIAGE:
				return getFoliageColourMultiplier(biome);
			case WATER:
				return getWaterColourMultiplier(biome);
			default:
				return 0xffffff;
		}
	}
	
	public void setBiomeWaterShading(int biomeID, int colour) {
		this.waterMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeGrassShading(int biomeID, int colour) {
		this.grassMultiplierArray[biomeID & 0xff] = colour;
	}
	
	public void setBiomeFoliageShading(int biomeID, int colour) {
		this.foliageMultiplierArray[biomeID & 0xff] = colour;
	}
	
	private static BlockType getBlockTypeFromString(String typeString) {

		if (typeString.equalsIgnoreCase("normal"))          { return BlockType.NORMAL;  }
		else if (typeString.equalsIgnoreCase("grass"))      { return BlockType.GRASS;   }
		else if (typeString.equalsIgnoreCase("leaves"))     { return BlockType.LEAVES;  }
		else if (typeString.equalsIgnoreCase("foliage"))    { return BlockType.FOLIAGE; }
		else if (typeString.equalsIgnoreCase("water"))      { return BlockType.WATER;   }
		else if (typeString.equalsIgnoreCase("opaque"))     { return BlockType.OPAQUE;  }
		else
		{
			RegionManager.logWarning("unknown block type '%s'", typeString);
		}

		return BlockType.NORMAL;
	}
	
	private static String getBlockTypeAsString(BlockType blockType)
	{
		switch (blockType)
		{
			case NORMAL:    return "normal";
			case GRASS:     return "grass";
			case LEAVES:    return "leaves";
			case FOLIAGE:   return "foliage";
			case WATER:     return "water";
			case OPAQUE:    return "opaque";
		}

		return "normal";
	}
	
	public BlockType getBlockType(int blockAndMeta) {
		return this.blockTypeArray[blockAndMeta & 0xffff];
	}
	
	public BlockType getBlockType(int blockId, int meta) {
		return this.blockTypeArray[((blockId & 0xfff) << 4) | (meta & 0xf)];
	}
	
	public void setBlockType(int blockId, int meta, BlockType type) {
		this.blockTypeArray[((blockId & 0xfff) << 4) | (meta & 0xf)] = type;
	}
	
	public void setBlockType(int blockAndMeta, BlockType type) {
		this.blockTypeArray[blockAndMeta & 0xffff] = type;
	}
	
	public static int getColourFromString(String s) {
		return (int) (Long.parseLong(s, 16) & 0xffffffffL);
	}
	
	/**
	 * Read biome color multiplier values.
	 * The line format is:<br>
	 * biome &lt;biomeId&gt; &lt;waterMultiplier&gt; &lt;grassMultiplier&gt; &lt;foliageMultiplier&gt;<br>
	 * The biome id, meta value, and color code are in hex.<br>
	 * Accepts "*" as a wildcard for biome id (meaning for all biomes).
	 * @param lineParts
	 */
	private void loadBiomeLine(String line)
	{
		try
		{
			int startBiomeId = 0;
			int endBiomeId = MAX_BIOMES;
			String waterMultiplierStr   = "ffffff";
			String grassMultiplierStr   = "ffffff";
			String foliageMultiplierStr = "ffffff";
			String[] lineParts = line.split(" ");

			if (lineParts[1].equals("*"))
			{
				waterMultiplierStr = lineParts[2];
				grassMultiplierStr = lineParts[3];
				foliageMultiplierStr = lineParts[4];
			}
			else
			{
				Matcher matcher = PATTERN_BIOME_LINE_ID.matcher(line);

				if (matcher.matches())
				{
					startBiomeId = Integer.parseInt(matcher.group(1));
				}
				else
				{
					matcher = PATTERN_BIOME_LINE_NAME.matcher(line);

					if (matcher.matches())
					{
						String idStr = matcher.group(1);
						Integer idInt = this.biomeIdMap.get(idStr);

						if (idInt == null)
						{
							throw new NumberFormatException("Missing biome ID map entry for '" + idStr + "'");
						}

						startBiomeId = idInt;
					}
					else
					{
						throw new NumberFormatException("Biome color line didn't match the regex");
					}
				}

				waterMultiplierStr = matcher.group(2);
				grassMultiplierStr = matcher.group(3);
				foliageMultiplierStr = matcher.group(4);

				endBiomeId = startBiomeId + 1;
			}
			
			if (startBiomeId >= 0 && startBiomeId < MAX_BIOMES)
			{
				int waterMultiplier = getColourFromString(waterMultiplierStr) & 0xffffff;
				int grassMultiplier = getColourFromString(grassMultiplierStr) & 0xffffff;
				int foliageMultiplier = getColourFromString(foliageMultiplierStr) & 0xffffff;
				
				for (int biomeId = startBiomeId; biomeId < endBiomeId; biomeId++)
				{
					this.setBiomeWaterShading(biomeId, waterMultiplier);
					this.setBiomeGrassShading(biomeId, grassMultiplier);
					this.setBiomeFoliageShading(biomeId, foliageMultiplier);
				}
			}
			else
			{
				RegionManager.logWarning("Biome ID '%d' out of range", startBiomeId);
			}
			
		}
		catch (NumberFormatException e)
		{
			RegionManager.logWarning("Invalid biome color line '%s'", line);
		}
	}
	
	/**
	 * Read block color values.
	 * The line format is:<br>
	 * block &lt;blockId&gt; &lt;blockMeta&gt; &lt;color&gt;<br>
	 * The biome id, meta value, and color code are in hex.<br>
	 * Accepts "*" as a wildcard for block id and meta values (meaning for all blocks and/or meta values).
	 * @param lineParts
	 * @param isBlockColourLine
	 */
	private void loadBlockLine(String[] lineParts, boolean isBlockColourLine)
	{
		try
		{
			int startBlockId = 0;
			int endBlockId = MAX_BLOCKS;

			if (lineParts[1].equals("*") == false)
			{
				Matcher matcher = PATTERN_BLOCK_STRING_ID.matcher(lineParts[1]);

				if (matcher.matches())
				{
					String idStr = matcher.group(1);
					Integer idInt = this.blockIdMap.get(idStr);

					if (idInt == null)
					{
						throw new NumberFormatException("Missing block ID map entry for '" + idStr + "'");
					}

					startBlockId = idInt;
				}
				else
				{
					startBlockId = Integer.parseInt(lineParts[1]);
				}

				endBlockId = startBlockId + 1;
			}
			
			int startBlockMeta = 0;
			int endBlockMeta = MAX_META;

			if (lineParts[2].equals("*") == false)
			{
				startBlockMeta = Integer.parseInt(lineParts[2]);
				endBlockMeta = startBlockMeta + 1;
			}
			
			if (startBlockId >= 0 && startBlockId < MAX_BLOCKS && startBlockMeta >= 0 && startBlockMeta < MAX_META)
			{
				if (isBlockColourLine)
				{
					// block color line
					int colour = getColourFromString(lineParts[3]);
					
					for (int blockId = startBlockId; blockId < endBlockId; blockId++)
					{
						for (int blockMeta = startBlockMeta; blockMeta < endBlockMeta; blockMeta++)
						{
							this.setColour(blockId, blockMeta, colour);
						}
					}
				}
				else
				{
					// block type line
					BlockType type = getBlockTypeFromString(lineParts[3]);
					
					for (int blockId = startBlockId; blockId < endBlockId; blockId++)
					{
						for (int blockMeta = startBlockMeta; blockMeta < endBlockMeta; blockMeta++)
						{
							this.setBlockType(blockId, blockMeta, type);
						}
					}
				}
			}
			
		}
		catch (NumberFormatException e)
		{
			System.err.printf("Error: Invalid block color line '%s'", String.join(" ", lineParts));
			e.printStackTrace();
		}
	}
	
	public void loadFromFile(File file)
	{
		Scanner scanner = null;

		try
		{
			scanner = new Scanner(new FileReader(file));
			
			while (scanner.hasNextLine())
			{
				// get next line and remove comments (part of line after #)
				String line = scanner.nextLine().split("#")[0].trim();

				if (line.length() > 0)
				{
					String[] lineSplit = line.split(" ");

					if (lineSplit[0].equals("biome") && lineSplit.length == 5)
					{
						this.loadBiomeLine(line);
					}
					else if (lineSplit[0].equals("block") && lineSplit.length == 4)
					{
						this.loadBlockLine(lineSplit, true);
					}
					else if (lineSplit[0].equals("blocktype") && lineSplit.length == 4)
					{
						this.loadBlockLine(lineSplit, false);
					}
					else
					{
						RegionManager.logWarning("invalid map colour line '%s'", line);
					}
				}
			}
		}
		catch (IOException e)
		{
			RegionManager.logError("Error loading block colours: no such file '%s'", file);
		}
		finally
		{
			if (scanner != null)
			{
				scanner.close();
			}
		}
	}
	
	//
	// Methods for saving block colours to file.
	//
	
	// save biome colour multipliers to a file.
	public void saveBiomes(Writer fout) throws IOException {
		fout.write("biome * ffffff ffffff ffffff\n");
		
		for (int biomeId = 0; biomeId < MAX_BIOMES; biomeId++) {
			int waterMultiplier = this.getWaterColourMultiplier(biomeId) & 0xffffff;
			int grassMultiplier = this.getGrassColourMultiplier(biomeId) & 0xffffff;
			int foliageMultiplier = this.getFoliageColourMultiplier(biomeId) & 0xffffff;
			
			// don't add lines that are covered by the default.
			if ((waterMultiplier != 0xffffff) || (grassMultiplier != 0xffffff) || (foliageMultiplier != 0xffffff)) {
				fout.write(String.format("biome %d %06x %06x %06x\n", biomeId, waterMultiplier, grassMultiplier, foliageMultiplier));
			}
		}
	}
	
	private static String getMostOccurringKey(Map<String, Integer> map, String defaultItem) {
		// find the most commonly occurring key in a hash map.
		// only return a key if there is more than 1.
		int maxCount = 1;
		String mostOccurringKey = defaultItem;
		for (Entry<String, Integer> entry : map.entrySet()) {
			String key = entry.getKey();
			int count = entry.getValue();
			
			if (count > maxCount) {
				maxCount = count;
				mostOccurringKey = key;
			}
		}
		
		return mostOccurringKey;
	}
	
	// to use the least number of lines possible find the most commonly occurring
	// item for the 16 different meta values of a block.
	// an 'item' is either a block colour or a block type.
	// the most commonly occurring item is then used as the wildcard entry for
	// the block, and all non matching items added afterwards.
	private static void writeMinimalBlockLines(Writer fout, String lineStart, String[] items, String defaultItem) throws IOException {
		
		Map<String, Integer> frequencyMap = new HashMap<String, Integer>();
		
		// first count the number of occurrences of each item.
		for (String item : items) {
			int count = 0;
			if (frequencyMap.containsKey(item)) {
				count = frequencyMap.get(item);
			}
			frequencyMap.put(item, count + 1);
		}
		
		// then find the most commonly occurring item.
		String mostOccurringItem = getMostOccurringKey(frequencyMap, defaultItem);
		
		// only add a wildcard line if it actually saves lines.
		if (!mostOccurringItem.equals(defaultItem)) {
			fout.write(String.format("%s * %s\n", lineStart, mostOccurringItem));
		}
		
		// add lines for items that don't match the wildcard line.
		for (int i = 0; i < items.length; i++) {
			if (!items[i].equals(mostOccurringItem) && !items[i].equals(defaultItem)) {
				fout.write(String.format("%s %d %s\n", lineStart, i, items[i]));
			}
		}
	}
	
	public void saveBlocks(Writer fout) throws IOException {
		fout.write("block * * 00000000\n");
		
		String[] colours = new String[MAX_META];
		
		for (int blockId = 0; blockId < MAX_BLOCKS; blockId++) {
			// build a 16 element list of block colours
			for (int meta = 0; meta < MAX_META; meta++) {
				colours[meta] = String.format("%08x", this.getColour(blockId, meta));
			}
			// write a minimal representation to the file
			String lineStart = String.format("block %d", blockId);
			writeMinimalBlockLines(fout, lineStart, colours, "00000000");
		}
	}
	
	public void saveBlockTypes(Writer fout) throws IOException {
		fout.write("blocktype * * normal\n");
		
		String[] blockTypes = new String[MAX_META];
		
		for (int blockId = 0; blockId < MAX_BLOCKS; blockId++) {
			// build a 16 element list of block types
			for (int meta = 0; meta < MAX_META; meta++) {
				BlockType bt = this.getBlockType(blockId, meta);
				blockTypes[meta] = getBlockTypeAsString(bt);
			}
			// write a minimal representation to the file
			String lineStart = String.format("blocktype %d", blockId);
			writeMinimalBlockLines(fout, lineStart, blockTypes, "normal");
		}
	}
	
	// save block colours and biome colour multipliers to a file.
	public void saveToFile(File f) {
		Writer fout = null;
		try {
			fout = new OutputStreamWriter(new FileOutputStream(f));
			this.saveBiomes(fout);
			this.saveBlockTypes(fout);
			this.saveBlocks(fout);
			
		} catch (IOException e) {
			RegionManager.logError("saving block colours: could not write to '%s'", f);
			
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {}
			}
		}
	}
	
	public static void writeOverridesFile(File f) {
		Writer fout = null;
		try {
			fout = new OutputStreamWriter(new FileOutputStream(f));
			
			fout.write(
				"block 37 * 60ffff00      # make dandelions more yellow\n" +
				"block 38 * 60ff0000      # make roses more red\n" +
				"blocktype 2 * grass      # grass block\n" +
				"blocktype 8 * water      # still water block\n" +
				"blocktype 9 * water      # flowing water block\n" +
				"blocktype 18 * leaves    # leaves block\n" +
				"blocktype 18 1 opaque    # pine leaves (not biome colorized)\n" +
				"blocktype 18 2 opaque    # birch leaves (not biome colorized)\n" +
				"blocktype 31 * grass     # tall grass block\n" +
				"blocktype 106 * foliage  # vines block\n" +
				"blocktype 169 * grass    # biomes o plenty holy grass\n" +
				"blocktype 1920 * grass   # biomes o plenty plant\n" +
				"blocktype 1923 * opaque  # biomes o plenty leaves 1\n" +
				"blocktype 1924 * opaque  # biomes o plenty leaves 2\n" +
				"blocktype 1925 * foliage # biomes o plenty foliage\n" +
				"blocktype 1926 * opaque  # biomes o plenty fruit leaf block\n" +
				"blocktype 1932 * foliage # biomes o plenty tree moss\n" +
				"blocktype 1962 * leaves  # biomes o plenty colorized leaves\n" +
				"blocktype 2164 * leaves  # twilight forest leaves\n" +
				"blocktype 2177 * leaves  # twilight forest magic leaves\n" +
				"blocktype 2204 * leaves  # extrabiomesXL green leaves\n" +
				"blocktype 2200 * opaque  # extrabiomesXL autumn leaves\n" +
				"blocktype 3257 * opaque  # natura berry bush\n" +
				"blocktype 3272 * opaque  # natura darkwood leaves\n" +
				"blocktype 3259 * leaves  # natura flora leaves\n" +
				"blocktype 3278 * opaque  # natura rare leaves\n" +
				"blocktype 3258 * opaque  # natura sakura leaves\n"
			);
			
		} catch (IOException e) {
			RegionManager.logError("saving block overrides: could not write to '%s'", f);
			
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (IOException e) {}
			}
		}
	}
}
