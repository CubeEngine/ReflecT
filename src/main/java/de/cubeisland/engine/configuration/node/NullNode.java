package de.cubeisland.engine.configuration.node;

public class NullNode extends Node
{
    private NullNode()
    {}

    public static NullNode emptyNode()
    {
        return new NullNode();
    }

    @Override
    public Object getValue()
    {
        return null;
    }

    @Override
    public String asText()
    {
        return "";
    }

    @Override
    public String toString()
    {
        return "NullNode";
    }
}
