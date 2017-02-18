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
package org.cubeengine.reflect.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.reflect.Section;
import org.cubeengine.reflect.SectionConverter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AssertionUtils
{
    public static <T> void assertEqualsDeep(ConverterManager converterManager, T expected, T actual)
    {
        Class clazz = expected.getClass();
        if (expected instanceof Map)
        {
            // It seems like Hocon lowercases the mapkeys.
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>)expected).entrySet())
            {
                if (((Map)actual).get(entry.getKey()) == null)
                {
                    assertEqualsDeep(converterManager, entry.getValue(), ((Map)actual).get(
                        ((String)entry.getKey()).toLowerCase()));
                }
                else
                {
                    assertEqualsDeep(converterManager, entry.getValue(), ((Map)actual).get(entry.getKey()));
                }
            }
        }
        else if (expected instanceof Collection)
        {
            Iterator expectedIterator = ((Collection)expected).iterator();
            Iterator actualIterator = ((Collection)actual).iterator();
            while (expectedIterator.hasNext())
            {
                assertEqualsDeep(converterManager, expectedIterator.next(), actualIterator.next());
            }
        }
        else if (expected instanceof Section)
        {
            SectionConverter sectionConverter = (SectionConverter)converterManager.matchConverter(Section.class);
            for (Field field : sectionConverter.getReflectedFields(((Section)expected).getClass()))
            {
                try
                {
                    assertEqualsDeep(converterManager, field.get(expected), field.get(actual));
                }
                catch (IllegalAccessException e)
                {
                    System.out.println("The section could not get its own field. This should never happen!");
                }
            }
        }
        else if (expected.getClass().isArray())
        {
            assertArrayEquals((Object[])expected, (Object[])actual);
        }
        else
        {
            assertEquals(expected, actual);
        }
    }
}
