package de.cubeisland.engine.configuration.node;

public class IntNode extends Node<Integer>
{

    private int value;

    public IntNode(int value)
    {
        this.value = value;
    }

    @Override
    public Integer getValue()
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
        return "IntNode=["+value+"]";
    }
}
