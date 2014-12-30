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
package de.cubeisland.engine.reflect.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.exception.ReflectedInstantiationException;

/**
 * This factory provides a Method to create a new Instance of a Section
 */
public class SectionFactory
{
    private SectionFactory()
    {
    }

    /**
     * Returns true if the {@link de.cubeisland.engine.reflect.Section} class <code>isAssignableFrom</code> the given class
     *
     * @param clazz the class to check
     *
     * @return true if the given class implements {@link de.cubeisland.engine.reflect.Section}
     */
    public static boolean isSectionClass(Class clazz)
    {
        return Section.class.isAssignableFrom(clazz);
    }

    /**
     * Creates a new Instance of the <code>sectionClass</code> using its default-constructor
     * <p>the <code>parent</code> is needed when trying to instantiating a non-static inner-class Section
     *
     * @param sectionClass the class of the Section to instantiate
     * @param parent       an instance of the enclosing class of the <code>sectionClass</code>
     *
     * @return the instantiated Section
     */
    public static Section newSectionInstance(Class<? extends Section> sectionClass, Object parent)
    {
        try
        {
            if (sectionClass.getEnclosingClass() == null)
            {
                return sectionClass.newInstance();
            }
            else if (Modifier.isStatic(sectionClass.getModifiers()))
            {
                return sectionClass.newInstance();
            }
            else
            {
                return sectionClass.getDeclaredConstructor(sectionClass.getEnclosingClass()).newInstance(parent);
            }
        }
        catch (InstantiationException e)
        {
            throw new ReflectedInstantiationException(sectionClass, e);
        }
        catch (InvocationTargetException e)
        {
            throw new ReflectedInstantiationException(sectionClass, e);
        }
        catch (NoSuchMethodException e)
        {
            throw new ReflectedInstantiationException(sectionClass, e);
        }
        catch (IllegalAccessException e)
        {
            throw new ReflectedInstantiationException(sectionClass, e);
        }
    }
}
