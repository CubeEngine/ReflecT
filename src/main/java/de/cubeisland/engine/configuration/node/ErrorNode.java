package de.cubeisland.engine.configuration.node;

public class ErrorNode extends Node<Void>
{
    private final String message;

    public ErrorNode(String message)
    {
        this.message = message;
    }

    public String getErrorMessage()
    {
        return message;
    }

    @Override
    public String asText()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Void getValue()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return "ErrorNode=["+message+"]";
    }
}
