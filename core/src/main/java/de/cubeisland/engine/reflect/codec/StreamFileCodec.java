package de.cubeisland.engine.reflect.codec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class StreamFileCodec extends FileCodec<InputStream, OutputStream>
{
    @Override
    public InputStream newInput(File f) throws IOException
    {
        return new FileInputStream(f);
    }

    @Override
    public OutputStream newOutput(File f) throws IOException
    {
        return new FileOutputStream(f);
    }
}
