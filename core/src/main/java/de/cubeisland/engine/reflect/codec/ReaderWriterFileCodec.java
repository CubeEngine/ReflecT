package de.cubeisland.engine.reflect.codec;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public abstract class ReaderWriterFileCodec extends FileCodec<Reader, Writer>
{
    @Override
    public Reader newInput(File f) throws IOException
    {
        return new FileReader(f);
    }

    @Override
    public Writer newOutput(File f) throws IOException
    {
        return null;
    }
}
