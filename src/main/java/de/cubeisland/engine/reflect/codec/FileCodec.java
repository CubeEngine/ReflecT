/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.exception.CodecIOException;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.ErrorNode;

public abstract class FileCodec extends Codec<InputStream, OutputStream>
{
    public final Collection<ErrorNode> loadReflected(Reflected reflected, InputStream is)
    {
        try
        {
            return dumpIntoSection(reflected.getDefault(), reflected, this.load(is, reflected), reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not load reflected", ex);
            }
            reflected.getLogger().warning("Could not load reflected" + ex);
            return Collections.emptyList();
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                reflected.getLogger().log(Level.WARNING, "Failed to close InputStream", e);
            }
        }
    }

    public final void saveReflected(Reflected reflected, OutputStream os)
    {
        try
        {
            this.save(convertSection(reflected.getDefault(), reflected, reflected), os, reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not save reflected", ex);
            }
            reflected.getLogger().warning("Could not save reflected" + ex);
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (IOException e)
            {
                reflected.getLogger().log(Level.WARNING, "Failed to close OutputStream", e);
            }
        }
    }

    /**
     * Returns the FileExtension as String
     *
     * @return the fileExtension
     */
    public abstract String getExtension();
}
