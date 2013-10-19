package de.cubeisland.engine.configuration.node;

public class ShortNode extends Node<Short>
{
    private short value;

    public ShortNode(short value)
    {
        this.value = value;
    }

    @Override
    public Short getValue()
    {
        return value;
    }

    @Override
    public String asText()
    {
        return String.valueOf(value);
    }

    @Override
    public String toString()
    {
        return "ShortNode=["+value+"]";
    }
}
