package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

public class StringConverter extends BasicConverter<String>
{
    @Override
    public String fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            return ((StringNode)node).getValue();
        }
        return node.asText();
    }
}
