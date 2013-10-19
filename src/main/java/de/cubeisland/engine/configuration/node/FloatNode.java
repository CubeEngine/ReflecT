package de.cubeisland.engine.configuration.node;

public class FloatNode extends Node<Float>
{
    private float value;

    public FloatNode(float value)
    {
        this.value = value;
    }

    @Override
    public Float getValue()
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
        return "FloatNode=["+value+"]";
    }
}
