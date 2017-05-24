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
package org.cubeengine.converter.converter;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;

public class ClassConverter extends SimpleConverter<Class<?>>
{
	@Override
	public Node toNode(Class<?> object) throws ConversionException
	{
		return StringNode.of(object.getName());
	}

	@Override
	public Class<?> fromNode(Node node) throws ConversionException
	{
		if (!(node instanceof StringNode))
		{
			throw ConversionException.of(this, node, "The node type isn't supported for a class.");
		}

		try
		{
			return this.getClass().getClassLoader().loadClass(((StringNode) node).getValue());
		}
		catch (ClassNotFoundException e)
		{
			throw ConversionException.of(this, node, "The class wasn't found.", e);
		}
	}
}
