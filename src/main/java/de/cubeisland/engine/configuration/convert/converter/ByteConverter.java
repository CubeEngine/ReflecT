
package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.ByteNode;
import de.cubeisland.engine.configuration.node.Node;

public class ByteConverter extends BasicConverter<Byte>
{
    @Override
    public Byte fromNode(Node node) throws ConversionException
    {
        if (node instanceof ByteNode)
        {
            return ((ByteNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Byte.parseByte(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
