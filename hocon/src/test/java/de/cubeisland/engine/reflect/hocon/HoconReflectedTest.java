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
package de.cubeisland.engine.reflect.hocon;

import de.cubeisland.engine.reflect.Reflector;
import de.cubeisland.engine.reflect.exception.DuplicatedPathException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static de.cubeisland.engine.reflect.hocon.ReflectedFieldShadowing.*;
import static org.junit.Assert.assertEquals;

public class HoconReflectedTest
{
    private ReflectedTest test1;
    private ReflectedTest2 test2;
    private File file;

    private Reflector factory;

    @Before
    public void setUp() throws Exception
    {
        this.file = new File("../testReflected.conf");
        this.factory = new Reflector();
        this.test1 = factory.create(ReflectedTest.class);
        this.test2 = factory.create(ReflectedTest2.class);
    }

    @Test
    public void test() throws Exception
    {
        ReflectedTest loadConfig = factory.load(ReflectedTest.class, file);
        file.delete();
        assertEquals(test1.getCodec().convertReflected(test1).asString(), test1.getCodec().convertReflected(loadConfig).asString());
    }

    @Test
    public void test2() throws Exception
    {
        test2.save(file);
        ReflectedTest2 loadConfig = factory.load(ReflectedTest2.class, file);
        file.delete();
        assertEquals(test2.getCodec().convertReflected(test2).asString(), test2.getCodec().convertReflected(loadConfig).asString());
    }

    @Test(expected = DuplicatedPathException.class)
    public void test3() throws Exception
    {
        factory.create(ReflectedFieldShadowing2.class);
    }
}