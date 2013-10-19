package de.cubeisland.engine.configuration.node;

public class ByteNode extends Node<Byte>
{
    private byte value;

    public ByteNode(byte value)
    {
        this.value = value;
    }

    @Override
    public Byte getValue()
    {
        return this.value;
    }

    @Override
    public String asText()
    {
        return String.valueOf(value);
    }

    @Override
    public String toString()
    {
        return "ByteNode=["+value+"]";
    }
}
