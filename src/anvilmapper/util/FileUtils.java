package anvilmapper.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import anvilmapper.AnvilMapper;

public class FileUtils
{
    public static final FilenameFilter ANVIL_REGION_FILE_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            //return name.startsWith("r.") && name.endsWith(".mca");
            try
            {
                return Pattern.matches("r\\.-?[0-9]+\\.-?[0-9]+\\.mca", name);
            }
            catch (PatternSyntaxException e)
            {
                AnvilMapper.LOGGER.severe("Failed build the regex pattern to match region files");
                e.printStackTrace();
                return false;
            }
        }
    };
}
