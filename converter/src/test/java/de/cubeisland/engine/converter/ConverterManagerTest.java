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
package de.cubeisland.engine.converter;

import java.lang.reflect.Type;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import de.cubeisland.engine.converter.node.Node;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConverterManagerTest
{
    private ConverterManager manager;

    @Before
    public void setUp() throws Exception
    {
        manager = ConverterManager.defaultManager();
    }

    @Test
    public void testSimpleConverters() throws ConversionException
    {
        // Noolean
        check(true);
        // Numbers:
        check(Byte.MAX_VALUE);
        check(Double.MAX_VALUE);
        check(Float.MAX_VALUE);
        check(Integer.MAX_VALUE);
        check(Long.MAX_VALUE);
        check(Short.MAX_VALUE);
        // String
        check("aString");
        // UUID
        check(UUID.randomUUID());
        // Date
        check(Date.valueOf("2014-12-27"));
        // Level
        check(Level.WARNING);
        // Class
        check(ConverterManagerTest.class);
    }

    List<String> list = new ArrayList<String>();
    Map<String, String> map = new HashMap<String, String>();
    String[] array = {"TEST"};

    // TODO test generic
    public void testGenericConverters() throws ConversionException
    {
        list.add("TEST");
        check(list); // TODO pass type
        check(array); // TODO pass type
        check(map); // TODO pass type
    }

    private void check(Object value) throws ConversionException
    {
        check(value, value.getClass());
    }

    private void check(Object value, Type type) throws ConversionException
    {
        Node node = manager.convertToNode(value);
        assertEquals(value, manager.convertFromNode(node, type));
    }
}
