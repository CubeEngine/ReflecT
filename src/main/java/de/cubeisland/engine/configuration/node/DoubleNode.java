package de.cubeisland.engine.configuration.node;

public class DoubleNode extends Node<Double>
{
    private double value;

    public DoubleNode(double value)
    {
        this.value = value;
    }

    @Override
    public Double getValue()
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
        return "DoubleNode=["+value+"]";
    }
}
