package de.cubeisland.engine.configuration.node;

public class BooleanNode extends Node<Boolean>
{
    private final boolean bool;

    public BooleanNode(boolean bool)
    {
        this.bool = bool;
    }

    @Override
    public Boolean getValue()
    {
        return this.bool;
    }

    @Override
    public String asText()
    {
        return String.valueOf(bool);
    }

    public static BooleanNode falseNode()
    {
        return new BooleanNode(false);
    }

    public static BooleanNode trueNode()
    {
        return new BooleanNode(true);
    }

    public static BooleanNode of(boolean bool)
    {
        return bool ? trueNode() : falseNode();
    }

    @Override
    public String toString()
    {
        return "BooleanNode=["+bool+"]";
    }
}
