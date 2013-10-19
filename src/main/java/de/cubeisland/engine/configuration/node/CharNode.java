package de.cubeisland.engine.configuration.node;

public class CharNode extends Node<Character>
{
    private char value;

    public CharNode(char value)
    {
        this.value = value;
    }

    @Override
    public Character getValue()
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
        return "CharNode=["+value+"]";
    }
}
