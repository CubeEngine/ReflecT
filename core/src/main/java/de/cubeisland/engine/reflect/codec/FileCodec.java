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
package de.cubeisland.engine.reflect.codec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.exception.CodecIOException;

import static de.cubeisland.engine.reflect.Reflector.LOGGER;

/**
 * A Codec using {@link InputStream} and {@link OutputStream} to save/load into/from a File
 */
public abstract class FileCodec<I, O> extends Codec<I, O>
{
    public final void loadReflected(Reflected reflected, I input)
    {
        try
        {
            this.fillReflected(reflected, this.load(input, reflected));
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not load reflected", ex);
            }
            LOGGER.warning("Could not load reflected" + ex);
        }
    }

    public final void saveReflected(Reflected reflected, O output)
    {
        try
        {
            this.save(convertReflected(reflected), output, reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not save reflected", ex);
            }
            LOGGER.warning("Could not save reflected" + ex);
        }
    }

    public abstract I newInput(File f) throws IOException;
    public abstract O newOutput(File f) throws IOException;

    /**
     * Returns the FileExtension as String
     *
     * @return the fileExtension
     */
    public abstract String getExtension();
}
