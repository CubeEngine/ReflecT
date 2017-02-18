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
package org.cubeengine.converter.converter;

import java.util.UUID;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;

/**
 * A Converter for {@link java.util.UUID}
 */
public class UUIDConverter extends SimpleConverter<UUID>
{
    @Override
    public Node toNode(UUID object) throws ConversionException
    {
        return StringNode.of(object.toString());
    }

    @Override
    public UUID fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            return UUID.fromString(node.asText());
        }
        throw ConversionException.of(this, node, "Node incompatible with UUID!");
    }
}
