package de.cubeisland.engine.configuration.codec;

import de.cubeisland.engine.configuration.Configuration;
import de.cubeisland.engine.configuration.node.MapNode;

public class TestCodec extends YamlCodec
{
    public MapNode convertConfiguration(Configuration config)
    {
        return convertSection(config, config, config);
    }
}
