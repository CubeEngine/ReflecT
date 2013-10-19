package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.BooleanNode;
import de.cubeisland.engine.configuration.node.Node;

public class BooleanConverter extends BasicConverter<Boolean>
{
    @Override
    public Boolean fromNode(Node node) throws ConversionException
    {
        if (node instanceof BooleanNode)
        {
            return ((BooleanNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            if (s == null)
            {
                return null;
            }
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("1"))
            {
                return true;
            }
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("0"))
            {
                return false;
            }
            return null;
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
