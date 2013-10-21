package de.cubeisland.engine.configuration;

import junit.framework.TestCase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class YamlConfigurationTest extends TestCase
{
    private TestConfig config;
    private TestConfig loadConfig;
    private Path path;

    @Override
    public void setUp() throws Exception
    {
        this.path = new File("../testconfig.yml").toPath();
        System.out.println(path.toAbsolutePath());
        config = Configuration.create(TestConfig.class);
    }

    public void testConfiguration() throws Exception
    {
        config.save(path);
        loadConfig = Configuration.load(TestConfig.class, path);

        Files.delete(path);
        assertEquals(config.getCodec().convertSection(config).toString(), config.getCodec().convertSection(loadConfig).toString());
    }
}
