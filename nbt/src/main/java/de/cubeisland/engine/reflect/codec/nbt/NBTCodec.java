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
package de.cubeisland.engine.reflect.codec.nbt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.BooleanNode;
import de.cubeisland.engine.converter.node.ByteNode;
import de.cubeisland.engine.converter.node.CharNode;
import de.cubeisland.engine.converter.node.DoubleNode;
import de.cubeisland.engine.converter.node.FloatNode;
import de.cubeisland.engine.converter.node.IntNode;
import de.cubeisland.engine.converter.node.ListNode;
import de.cubeisland.engine.converter.node.LongNode;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.NullNode;
import de.cubeisland.engine.converter.node.ShortNode;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.codec.StreamFileCodec;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.DoubleTag;
import org.spout.nbt.EndTag;
import org.spout.nbt.FloatTag;
import org.spout.nbt.IntTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.LongTag;
import org.spout.nbt.ShortTag;
import org.spout.nbt.StringTag;
import org.spout.nbt.Tag;
import org.spout.nbt.stream.NBTInputStream;
import org.spout.nbt.stream.NBTOutputStream;
import org.spout.nbt.util.NBTMapper;

public class NBTCodec extends StreamFileCodec
{
    @Override
    public String getExtension()
    {
        return "dat";
    }

    @Override
    protected final void save(MapNode node, OutputStream writer, Reflected config) throws ConversionException
    {
        try
        {
            NBTOutputStream nbtOutputStream = new NBTOutputStream(writer, false);
            nbtOutputStream.writeTag(this.convertMap(node));
            nbtOutputStream.flush();
            nbtOutputStream.close();
        }
        catch (IOException e)
        {
            throw ConversionException.of(this, null, "Could not write into NBTOutputStream", e);
        }
    }

    @Override
    protected final MapNode load(InputStream is, Reflected config)
    {
        try
        {
            NBTInputStream nbtInputStream = new NBTInputStream(is, false);
            Tag tag = nbtInputStream.readTag();
            CompoundMap tags = NBTMapper.toTagValue(tag, CompoundMap.class, null);
            return this.toMapNode(tags);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private MapNode toMapNode(CompoundMap tags)
    {
        MapNode values = MapNode.emptyMap();
        for (Entry<String, Tag<?>> entry : tags.entrySet())
        {
            values.set(entry.getKey(), this.toNode(entry.getValue()));
        }
        return values;
    }

    private void toMapNode(MapNode values, CompoundMap tags)
    {
        for (Entry<String, Tag<?>> entry : tags.entrySet())
        {
            values.set(entry.getKey(), this.toNode(entry.getValue()));
        }
    }

    private Node toNode(Object value)
    {
        if (value instanceof Tag)
        {
            if (value instanceof CompoundTag)
            {
                MapNode mapNode = MapNode.emptyMap();
                this.toMapNode(mapNode,((CompoundTag)value).getValue());
                return mapNode;
            }
            else if (value instanceof ListTag)
            {
                ListNode listNode = ListNode.emptyList();
                for (Object o : ((ListTag)value).getValue())
                {
                    listNode.addNode(this.toNode(o));
                }
                return listNode;
            }
            else if (value instanceof ByteTag
                || value instanceof StringTag
                || value instanceof DoubleTag
                || value instanceof FloatTag
                || value instanceof IntTag
                || value instanceof LongTag
                || value instanceof ShortTag)
            {
                try
                {
                    return this.getConverterManager().convertToNode(((Tag)value).getValue());
                }
                catch (ConversionException e)
                {
                    throw new IllegalStateException("Could not convert a value!", e);
                }
            }
            else if (value instanceof EndTag)
            {
                return NullNode.emptyNode();
            }
        }
        throw new IllegalStateException("Unknown Tag! "+ value.getClass().getName());
    }

    private CompoundTag convertMap(MapNode baseNode)
    {
        Map<String,Node> map = baseNode.getValue();
        CompoundTag result = new CompoundTag("root", new CompoundMap());
        if (map.isEmpty()) return result;
        this.convertMap(result.getValue(),map, baseNode);
        return result;
    }

    private void convertMap(CompoundMap rootMap, Map<String,Node> map, MapNode base)
    {
        for (Entry<String, Node> entry : map.entrySet())
        {
            rootMap.put(this.convertValue(base.getOriginalKey(entry.getKey()),entry.getValue()));
        }
    }

    private Tag<?> convertValue(String name, Node value)
    {
        if (value instanceof MapNode)
        {
            CompoundMap map = new CompoundMap();
            this.convertMap(map,((MapNode)value).getValue(), (MapNode)value);
            return new CompoundTag(name,map);
        }
        else if (value instanceof ListNode)
        {
            List<Tag> tagList = new ArrayList<Tag>();
            java.lang.Integer i = 0;
            for (Node node : ((ListNode)value).getValue())
            {
                i++;
                tagList.add(this.convertValue(i.toString(),node));
            }
            if (tagList.size() == 0)
            {
                return new ListTag(name, CompoundTag.class, tagList);
            }
            return new ListTag(name, tagList.get(0).getClass(), tagList);
        }
        else if (value instanceof BooleanNode)
        {
            return new ByteTag(name, (Boolean)value.getValue());
        }
        else if (value instanceof ByteNode)
        {
            return new ByteTag(name, (Byte)value.getValue());
        }
        else if (value instanceof CharNode)
        {
            return new StringTag(name, value.getValue().toString());
        }
        else if (value instanceof DoubleNode)
        {
            return new DoubleTag(name, (Double)value.getValue());
        }
        else if (value instanceof FloatNode)
        {
            return new FloatTag(name, (Float)value.getValue());
        }
        else if (value instanceof IntNode)
        {
            return new IntTag(name, (Integer)value.getValue());
        }
        else if (value instanceof LongNode)
        {
            return new LongTag(name, (Long)value.getValue());
        }
        else if (value instanceof ShortNode)
        {
            return new ShortTag(name, (Short)value.getValue());
        }
        else if (value instanceof StringNode)
        {
            return new StringTag(name, (String)value.getValue());
        }
        else if (value instanceof NullNode)
        {
            return new EndTag();
        }
        throw new IllegalStateException("Unknown Node! "+ value.getClass().getName());
    }
}
