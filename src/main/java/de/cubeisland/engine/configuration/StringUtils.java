package de.cubeisland.engine.configuration;

public class StringUtils {

    /**
     * This method merges an array of strings to a single string
     *
     * @param delimiter   the delimiter
     * @param strings the strings to implode
     * @return the imploded string
     */
    public static String implode(String delimiter, String[] strings)
    {
        if (strings.length == 0)
        {
            return "";
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            for (String s : strings)
            {
                sb.append(s);
            }
            return sb.toString();
        }
    }
}
