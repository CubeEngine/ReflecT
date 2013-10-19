package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.FloatNode;
import de.cubeisland.engine.configuration.node.Node;

public class FloatConverter extends BasicConverter<Float>
{
    @Override
    public Float fromNode(Node node) throws ConversionException
    {
        if (node instanceof FloatNode)
        {
            return ((FloatNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Float.parseFloat(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
