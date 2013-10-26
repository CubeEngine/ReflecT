package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

import java.util.UUID;

public class UUIDConverter implements Converter<UUID>
{
    public Node toNode(UUID object) throws ConversionException
    {
        return StringNode.of(object.toString());
    }

    public UUID fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            UUID.fromString(node.asText());
        }
        throw new ConversionException("Cannot convert " + node + " into UUID!");
    }
}
