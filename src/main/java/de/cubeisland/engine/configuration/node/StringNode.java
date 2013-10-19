package de.cubeisland.engine.configuration.node;

public class StringNode extends Node<String>
{
    private String value;

    public StringNode(String string)
    {
        this.value = string;

    }

    @Override
    public String getValue()
    {
        return value;
    }

    public void setValue(String string)
    {
        this.value = string;
    }

    @Override
    public String asText()
    {
        return value;
    }

    public static StringNode of(String string)
    {
        return new StringNode(string);
    }

    @Override
    public String toString()
    {
        return "StringNode=["+value+"]";
    }
}
