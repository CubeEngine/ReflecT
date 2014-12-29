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
package de.cubeisland.engine.converter.converter;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.Node;

import static de.cubeisland.engine.converter.node.Node.wrapIntoNode;

/**
 * Implements basic conversion of Strings, Numbers or Primitives to Node
 */
public abstract class BasicConverter<T> extends SimpleConverter<T>
{
    @SuppressWarnings("unchecked")
    @Override
    public Node toNode(T object) throws ConversionException
    {
        Class<T> clazz = (Class<T>)object.getClass();
        if (clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) ||
            CharSequence.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz))
        {
            return wrapIntoNode(object);
        }
        throw ConversionException.of(this, object, "Object is not a primitive, Number, CharSequence or Boolean");
    }
}
