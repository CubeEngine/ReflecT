/**
 * The MIT License
 * Copyright (c) 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.yaml;

import java.io.File;

import de.cubeisland.engine.reflect.Reflector;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YamlReflectedTest
{
    private ReflectedTest config;
    private ReflectedTest loadConfig;
    private File file;

    private Reflector factory;

    @Before
    public void setUp() throws Exception
    {
        this.file = new File("../testReflected.yml");
        factory = new Reflector();
        config = factory.create(ReflectedTest.class);
    }

    @Test
    public void testReflectedYaml() throws Exception
    {
        config.save(file);
        loadConfig = factory.load(ReflectedTest.class, file);
        file.delete();
        assertEquals(config.getCodec().convertReflected(config).toString(), config.getCodec().convertReflected(loadConfig).toString());
    }
}
