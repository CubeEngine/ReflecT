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
package de.cubeisland.engine.reflect.codec.mongo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBRefBase;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.node.ListNode;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.NullNode;
import de.cubeisland.engine.converter.node.ContainerNode;
import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.Codec;
import de.cubeisland.engine.reflect.codec.mongo.node.DBRefBaseNode;
import de.cubeisland.engine.reflect.codec.mongo.node.DateNode;
import de.cubeisland.engine.reflect.codec.mongo.node.ObjectIdNode;
import de.cubeisland.engine.reflect.exception.CodecIOException;
import org.bson.types.ObjectId;

public class MongoDBCodec extends Codec<DBObject, DBObject>
{
    @Override
    protected void onInit()
    {
        final ConverterManager cm = getConverterManager();
        cm.registerConverter(new DateConverter(), Date.class);
        cm.registerConverter(new ReferenceConverter(getReflector()), Reference.class);
    }

    @Override
    public void loadReflected(Reflected reflected, DBObject dbo)
    {
        try
        {
            fillReflected(reflected, this.load(dbo, reflected));
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not load reflected", ex);
            }
            Reflector.LOGGER.warning("Could not load reflected" + ex);
        }
    }

    @Override
    public void saveReflected(Reflected reflected, DBObject dbo)
    {
        try
        {
            this.save(convertReflected(reflected), dbo, reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not save reflected", ex);
            }
            Reflector.LOGGER.warning("Could not save reflected" + ex);
        }
    }

    @Override
    protected void save(MapNode mapNode, DBObject dbo, Reflected reflected) throws ConversionException
    {
        this.convertMapNode(dbo, mapNode);
    }

    private DBObject convertMapNode(DBObject dbo, MapNode mapNode)
    {
        if (mapNode.isEmpty())
        {
            return dbo;
        }
        for (Entry<String, Node> entry : mapNode.getMappedNodes().entrySet())
        {
            if (!(entry.getValue() instanceof NullNode))
            {
                dbo.put(entry.getKey(), convertNode(entry.getValue()));
            }
        }
        return dbo;
    }

    private Object convertNode(Node node)
    {
        if (node instanceof ContainerNode)
        {
            if (node instanceof MapNode)
            {
                return convertMapNode(new BasicDBObject(), (MapNode)node);
            }
            else if (node instanceof ListNode)
            {
                return convertListNode((ListNode)node);
            }
            else
            {
                throw new IllegalArgumentException("ParentNode has to be List or MapNode not a " + node.getClass());
            }
        }
        else
        {
            return node.getValue();
        }
    }

    private List<Object> convertListNode(ListNode listNode)
    {
        List<Object> list = new ArrayList<Object>();
        if (listNode.isEmpty())
        {
            return list;
        }
        for (Node node : listNode.getValue())
        {
            list.add(convertNode(node));
        }
        return list;
    }

    @Override
    protected MapNode load(DBObject dbo, Reflected reflected) throws ConversionException
    {
        return convertDBObjectToNode(dbo);
    }

    private MapNode convertDBObjectToNode(DBObject dbObject) throws ConversionException
    {
        MapNode mapNode = MapNode.emptyMap();
        for (String key : dbObject.keySet())
        {
            Object value = dbObject.get(key);
            Node nodeValue = this.convertObjectToNode(value);
            if (!(nodeValue instanceof NullNode))
            {
                mapNode.set(key, nodeValue);
            }
        }
        return mapNode;
    }

    private Node convertObjectToNode(Object value) throws ConversionException
    {
        Node nodeValue;
        if (value instanceof List)
        {
            nodeValue = this.convertListToNode((List)value);
        }
        else if (value instanceof DBObject)
        {
            nodeValue = this.convertDBObjectToNode((DBObject)value);
        }
        else if (value instanceof ObjectId)
        {
            nodeValue = new ObjectIdNode((ObjectId)value);
        }
        else if (value instanceof DBRefBase)
        {
            nodeValue = new DBRefBaseNode((DBRefBase)value);
        }
        else if (value instanceof Date)
        {
            nodeValue = new DateNode((Date)value);
        }
        else
        {
            nodeValue = getConverterManager().convertToNode(value);
        }
        return nodeValue;
    }

    private Node convertListToNode(List list) throws ConversionException
    {
        ListNode listNode = ListNode.emptyList();
        for (Object value : list)
        {
            listNode.addNode(this.convertObjectToNode(value));
        }
        return listNode;
    }
}
