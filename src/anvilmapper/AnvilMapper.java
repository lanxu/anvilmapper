package anvilmapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import anvilmapper.util.FileUtils;
import anvilmapper.util.IdMaps;
import anvilmapper.util.IdMaps.MapType;
import mapwriter.region.BlockColours;
import mapwriter.region.Region;
import mapwriter.region.RegionManager;

/* TODO:
 *  - Make it possible to load BlockColours from a file (make BlockColours Serializable?)
 *  - Load waypoints and output in JSON format? (No built in library for JSON, maybe use XML or CSV instead?)
 */

public class AnvilMapper
{
	private static final String DEFAULT_FILE_BLOCK_COLORS = "MapWriterBlockColours.txt";
	private static final String DEFAULT_FILE_BLOCK_ID_MAP = "block_ids.txt";
	private static final String DEFAULT_FILE_BIOME_ID_MAP = "biome_ids.txt";
	private static final String DEFAULT_DIR_WORLD = "world";
	public static final Logger LOGGER = Logger.getLogger("anvilmapper");
	private final File worldDir;
	private final File imageDir;
	private final BlockColours blockColours;
	private final RegionManager regionManager;
	private final boolean verbose;

	static
	{
		RegionManager.logger = LOGGER;
	}

	public AnvilMapper(File worldDir, File imageDir, File blockColoursFile, Map<String, Integer> biomeIdMap, Map<String, Integer> blockIdMap, boolean verbose)
	{
		this.worldDir = worldDir;
		this.imageDir = imageDir;
		this.verbose = verbose;
		this.blockColours = new BlockColours(biomeIdMap, blockIdMap);
		this.blockColours.loadFromFile(blockColoursFile);
		this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours);
	}

	public void processDimension(File dimDir, int dimension)
	{
		File regionDir = new File(dimDir, "region");

		if (dimDir.isDirectory())
		{
			File[] regionFilesList = regionDir.listFiles(FileUtils.ANVIL_REGION_FILE_FILTER);

			if (regionFilesList != null)
			{
				for (File regionFileName : regionFilesList)
				{
					if (regionFileName.isFile())
					{
						// get the region x and z from the region file name
						String[] baseNameSplit = regionFileName.getName().split("\\.");

						if ((baseNameSplit.length == 4) && (baseNameSplit[0].equals("r")) && (baseNameSplit[3].equals("mca")))
						{
							try
							{
								int rX = Integer.parseInt(baseNameSplit[1]);
								int rZ = Integer.parseInt(baseNameSplit[2]);

								Region region = this.regionManager.getRegion(rX << Region.SHIFT, rZ << Region.SHIFT, 0, dimension);

								if (this.verbose)
								{
									System.out.printf("DIM%d: Loaded file %s as region %s\n", dimension, regionFileName, region.toStringNoDim());
								}

								region.reload();
								region.updateZoomLevels();
								region.saveToImage();
								this.splitRegionImage(region, 1);
								this.regionManager.unloadRegion(region);
							}
							catch (NumberFormatException e)
							{
								RegionManager.logWarning("Could not get region x and z for region file %s", regionFileName);
							}
						}
						else
						{
							RegionManager.logWarning("Region file %s did not pass the file name check", regionFileName);
						}
					}
				}

				RegionManager.logInfo("Closing region manager");
				this.regionManager.close();

			}
			else
			{
				RegionManager.logInfo("No region files found for dimension %d", dimension);
			}
		}
		else
		{
			RegionManager.logInfo("No region directory in dimension directory %s", dimDir);
		}
	}

	public void processWorld()
	{
		File[] dimDirList = this.worldDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String name)
			{
				return f.isDirectory() && name.startsWith("DIM");
			}
		});

		for (File dimDir : dimDirList)
		{
			try
			{
				int dimension = Integer.parseInt(dimDir.getName().substring(3));
				this.processDimension(dimDir, dimension);
			}
			catch (NumberFormatException e)
			{
				RegionManager.logWarning("Failed to parse dimension number for dimension directory '%s'", dimDir);
			}
		}

		this.processDimension(this.worldDir, 0);
	}

	public static void writeImage(BufferedImage img, File imageFile)
	{
		// write the given image to the image file
		File dir = imageFile.getParentFile();

		if (!dir.exists())
		{
			dir.mkdirs();
		}

		try
		{
			ImageIO.write(img, "png", imageFile);
		}
		catch (IOException e)
		{
			RegionManager.logError("could not write image to %s", imageFile);
		}
	}

	private void splitRegionImage(Region region, int z) {
		int splitSize = Region.SIZE >> z;
		int[] pixels = region.getPixels();

		if (pixels != null)
		{
			BufferedImage regionImage = new BufferedImage(Region.SIZE, Region.SIZE, BufferedImage.TYPE_INT_RGB);
			regionImage.setRGB(0, 0, Region.SIZE, Region.SIZE, pixels, 0, Region.SIZE);

			BufferedImage dstImage = new BufferedImage(Region.SIZE, Region.SIZE, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = dstImage.createGraphics();

			for (int srcZ = 0; srcZ < Region.SIZE; srcZ += splitSize)
			{
				for (int srcX = 0; srcX < Region.SIZE; srcX += splitSize)
				{
					g.setPaint(Color.BLACK);
					g.fillRect(0, 0, Region.SIZE, Region.SIZE);
					g.drawImage(regionImage, 0, 0, Region.SIZE, Region.SIZE, srcX, srcZ, srcX + splitSize, srcZ + splitSize, null);
					writeImage(dstImage, Region.getImageFile(this.imageDir, region.x + srcX, region.z + srcZ, -z, region.dimension));
				}
			}
			g.dispose();
		}
	}

	public static void main(String [] args)
	{
		if (args.length < 1)
		{
			RegionManager.logInfo("usage: java AnvilMapper" +
							" [--world /path/to/world_save_directory]" +
							" [--out /path/to/output_directory]" +
							" [--block-colors /path/to/block_colors_file]" +
							" [--block-id-map /path/to/block_id_map]" +
							" [--biome-id-map /path/to/biome_id_map]" +
							" [--verbose]");
			RegionManager.logInfo("The default locations are:\n" +
								"  block-colors = MapWriterBlockColours.txt\n" +
								"  block-id-map = block_ids.txt\n" +
								"  biome_id_map = biome_ids.txt");
			RegionManager.logInfo("The block and biome ID map files are needed if the Block Colours\n" +
								"  file uses string names (as it does in the recent version of MapWriter),\n" +
								"  and if there is no block ID and biome ID map in the level.dat file\n" +
								"  (as there isn't in vanilla worlds).\n" +
								"  You can get the block and biome ID map files using the TellMe mod, and there are also\n" +
								"  ready made ID map files for vanilla Minecraft at <add URL here>.");

			return;
		}

		String outputLocation = null;
		String worldLocation = DEFAULT_DIR_WORLD;
		String blockColorsLocation = DEFAULT_FILE_BLOCK_COLORS;
		String biomeIdMapLocation = DEFAULT_FILE_BIOME_ID_MAP;
		String blockIdMapLocation = DEFAULT_FILE_BLOCK_ID_MAP;
		boolean verbose = false;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].startsWith("--"))
			{
				String argValue = null;

				if ((argValue = getArgumentValue(args, "world", i)) != null)
				{
					worldLocation = argValue;
				}
				else if ((argValue = getArgumentValue(args, "out", i)) != null)
				{
					outputLocation = argValue;
				}
				else if ((argValue = getArgumentValue(args, "biome-id-map", i)) != null)
				{
					biomeIdMapLocation = argValue;
				}
				else if ((argValue = getArgumentValue(args, "block-id-map", i)) != null)
				{
					blockIdMapLocation = argValue;
				}
				else if ((argValue = getArgumentValue(args, "block-colors", i)) != null)
				{
					blockColorsLocation = argValue;
				}
				else if (args[i].equals("--verbose"))
				{
					verbose = true;
				}
			}
		}

		File worldDir = new File(worldLocation);
		File imageDir = outputLocation != null ? new File(outputLocation, "images") : new File("images");
		File blockColorsFile = new File(blockColorsLocation);
		File blockIdMapFile = new File(blockIdMapLocation);
		File biomeIdMapFile = new File(biomeIdMapLocation);

		if (blockColorsFile.isFile() == false)
		{
			RegionManager.logError("The block colors file '%s' does not exist\n", blockColorsFile);
		}

		if (worldDir.isDirectory() == false)
		{
			RegionManager.logError("The world directory '%s' does not exist\n", worldDir);
			return;
		}

		if (imageDir.exists() == false && imageDir.mkdirs() == false)
		{
			RegionManager.logError("Failed to create the output image directory '%s'\n", imageDir);
			return;
		}

		Map<String, Integer> biomeIdMap = IdMaps.getIdMap(MapType.BIOMES, worldDir, biomeIdMapFile);
		Map<String, Integer> blockIdMap = IdMaps.getIdMap(MapType.BLOCKS, worldDir, blockIdMapFile);

		AnvilMapper anvilMapper = new AnvilMapper(worldDir, imageDir, blockColorsFile, biomeIdMap, blockIdMap, verbose);
		anvilMapper.processWorld();
	}

	private static String getArgumentValue(String[] args, String argName, int argIndex)
	{
		if (args.length > argIndex && args[argIndex].startsWith("--"))
		{
			String arg = args[argIndex].substring(2, args[argIndex].length());

			if (arg.equals(argName) && args.length > (argIndex + 1))
			{
				return args[argIndex + 1];
			}
			else if (arg.startsWith(argName) && arg.charAt(argName.length()) == '=')
			{
				return arg.substring(argName.length() + 1, arg.length());
			}
		}

		return null;
	}
}
