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

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.converter.SimpleConverter;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.codec.mongo.node.DBRefBaseNode;

public class ReferenceConverter extends SimpleConverter<Reference>
{
    private final Reflector reflector;

    public ReferenceConverter(Reflector reflector)
    {
        this.reflector = reflector;
    }

    @Override
    public Node toNode(Reference object) throws ConversionException
    {
        return new DBRefBaseNode(object.getDBRef());
    }

    @Override
    public Reference fromNode(Node node) throws ConversionException
    {
        if (node instanceof DBRefBaseNode)
        {
            return new Reference(reflector, ((DBRefBaseNode)node).getValue());
        }
        throw ConversionException.of(this, node, "Node is not DBRefBaseNode!");
    }
}
