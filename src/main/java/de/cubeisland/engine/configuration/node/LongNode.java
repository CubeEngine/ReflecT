package de.cubeisland.engine.configuration.node;

public class LongNode extends Node<Long>
{
    private long value;

    public LongNode(long value)
    {
        this.value = value;
    }

    @Override
    public Long getValue()
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
        return "LongNode=["+value+"]";
    }
}
