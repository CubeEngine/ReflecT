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
package de.cubeisland.engine.reflect.codec.hocon;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.codec.FileCodec;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Codec using the HOCON format
 */
public class HoconCodec extends FileCodec
{
    @Override
    public String getExtension() {
        return "conf";
    }

    // Reflected loading Method
    @Override
    @SuppressWarnings("unchecked")
    protected MapNode load(InputStream in, Reflected reflected) throws ConversionException {
        if (in == null)
        {
            // InputStream null -> reflected was not existent
            return MapNode.emptyMap();
        }
        Config config = ConfigFactory.parseReader(new InputStreamReader(in));
        if (config.isEmpty())
        {
            // loadValues null -> reflected exists but was empty
            return MapNode.emptyMap();
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for(Map.Entry<String, ConfigValue> entry : config.entrySet()) {
            map.put(entry.getKey(), entry.getValue().unwrapped());
        }
        return (MapNode)reflected.getCodec().getConverterManager().convertToNode(map);
    }

    // Reflected saving Methods
    @Override
    protected void save(MapNode node, OutputStream out, Reflected reflected) throws ConversionException {

    }
}
