/*
 * The MIT License
 * Copyright © 2013 Cube Island
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
package org.cubeengine.reflect.codec.hocon;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.reflect.Reflected;
import org.cubeengine.reflect.codec.ReaderWriterFileCodec;

/**
 * A Codec using the HOCON format
 */
public class HoconCodec extends ReaderWriterFileCodec
{
    @Override
    public String getExtension()
    {
        return "conf";
    }

    // Reflected loading Method
    @Override
    @SuppressWarnings("unchecked")
    protected MapNode load(Reader in, Reflected reflected) throws ConversionException
    {
        if (in == null)
        {
            // InputStream null -> reflected was not existent
            return MapNode.emptyMap();
        }
        Config config = ConfigFactory.parseReader(in);
        if (config.isEmpty())
        {
            // loadValues null -> reflected exists but was empty
            return MapNode.emptyMap();
        }
        return (MapNode)this.getConverterManager().convertToNode(config.root().unwrapped());
    }

    // Reflected saving Methods
    @Override
    protected void save(MapNode node, Writer writer, Reflected reflected) throws ConversionException
    {
        Config config = ConfigFactory.parseMap(getHoconMap(node));
        try
        {
            writer.append(config.root().render());
        }
        catch (IOException ex)
        {
            throw ConversionException.of(this, null, "Could not write", ex);
        }
    }

    private Map<String, Object> getHoconMap(MapNode node)
    {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        buildHoconMap(map, "", node);
        return map;
    }

    private void buildHoconMap(Map<String, Object> map, String path, Node node)
    {
        if (node instanceof MapNode)
        {
            if (((MapNode)node).isEmpty())
            {
                map.put(path, new LinkedHashMap<String, Object>());
            }
            else
            {
                for (Map.Entry<String, Node> entry : ((MapNode)node).getMappedNodes().entrySet())
                {
                    buildHoconMap(map, path + ("".equals(path) ? "" : ".") + entry.getKey(), entry.getValue());
                }
            }
        }
        else if (node instanceof ListNode)
        {
            map.put(path, getHoconList((ListNode)node));
        }
        else
        {
            map.put(path, node.getValue());
        }
    }

    protected List<Object> getHoconList(ListNode listNode)
    {
        List<Object> list = new LinkedList<Object>();
        for (Node node : listNode.getValue())
        {
            if (node instanceof MapNode)
            {
                list.add(getHoconMap((MapNode)node));
            }
            else if (node instanceof ListNode)
            {
                list.add(getHoconList((ListNode)node));
            }
            else
            {
                list.add(node.getValue());
            }
        }
        return list;
    }
}
