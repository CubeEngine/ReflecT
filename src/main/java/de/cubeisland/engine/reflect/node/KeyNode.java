package de.cubeisland.engine.reflect.node;

/**
 * KeyNodes can be used as Key in MapNodes
 *
 * @param <V>
 */
public abstract class KeyNode<V> extends Node<V>
{
    public String toKey()
    {
        return String.valueOf(this.getValue());
    }
}
