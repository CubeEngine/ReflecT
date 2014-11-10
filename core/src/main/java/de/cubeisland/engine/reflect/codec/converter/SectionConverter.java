/**
 * The MIT License
 * Copyright (c) 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.codec.converter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.exception.FieldAccessException;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;
import de.cubeisland.engine.reflect.node.ErrorNode;
import de.cubeisland.engine.reflect.node.MapNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.NullNode;
import de.cubeisland.engine.reflect.node.ReflectedPath;
import de.cubeisland.engine.reflect.util.SectionFactory;
import de.cubeisland.engine.reflect.util.StringUtils;

import static java.util.logging.Level.FINE;

/**
 * A converter for Sections.
 * <p>
 * This converter will cache the Fields of Sections to speed up repeated saving and loading of the same section
 */
public class SectionConverter
{
    private static final String[] NO_COMMENT = new String[0];
    private final Map<Field, ReflectedPath> paths = new HashMap<Field, ReflectedPath>();
    private final Map<Class<? extends Section>, Field[]> cachedFields = new HashMap<Class<? extends Section>, Field[]>();
    private final Map<Field, String[]> comments = new HashMap<Field, String[]>();

    /**
     * Detects if given field needs to be serialized
     * <p>static and transient Field do not get converted
     *
     * @param field the field to check
     *
     * @return whether the field is a field of the reflected that needs to be serialized
     */
    public static boolean isReflectedField(Field field)
    {
        int modifiers = field.getModifiers();
        return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers));
    }

    /**
     * Constructs a path for given Field
     *
     * @param field the field to get the path for
     *
     * @return the ReflectedPath
     */
    protected final ReflectedPath getPathFor(Field field)
    {
        ReflectedPath path = this.paths.get(field);
        if (path == null)
        {
            if (field.isAnnotationPresent(Name.class))
            {
                path = ReflectedPath.forName(field.getAnnotation(Name.class).value());
            }
            else
            {
                path = ReflectedPath.forName(StringUtils.fieldNameToPath(field.getName()));
            }
            this.paths.put(field, path);
        }
        return path;
    }

    /**
     * Converts a Section into a MapNode
     *
     * @param section the section to convert
     * @param manager the ConverterManager
     *
     * @return the converted Section
     */
    public Node toNode(Section section, ConverterManager manager) throws ConversionException
    {
        MapNode baseNode = MapNode.emptyMap();
        Class<? extends Section> sectionClass = section.getClass();

        for (Field field : this.getReflectedFields(sectionClass))
        {
            if (manager.getReflected().isInheritedField(field))
            {
                continue; // do not save inherited field of child config
            }
            try
            {
                Node newNode = manager.convertToNode(field.get(section));
                addComment(newNode, field);

                Node prevNode = baseNode.getNodeAt(getPathFor(field));
                if (prevNode instanceof MapNode)
                {
                    if (newNode instanceof MapNode)
                    {
                        for (Entry<String, Node> entry : ((MapNode)newNode).getMappedNodes().entrySet())
                        {
                            ((MapNode)prevNode).setExactNode(entry.getKey(), entry.getValue());
                        }
                    }
                }
                else
                {
                    baseNode.setNodeAt(getPathFor(field), newNode);
                }
            }
            catch (Exception e)
            {
                this.handleException(e, section, field);
            }
        }
        if (manager.getReflected().isChild())
        {
            // remove generated empty ParentNodes ONLY from child-reflected
            baseNode.cleanUpEmptyNodes();
        }
        return baseNode;
    }

    /**
     * Adds a comment to the given Node
     *
     * @param node  the Node to add the comment to
     * @param field the field possibly having a {@link de.cubeisland.engine.reflect.annotations.Comment} annotation
     */
    private void addComment(Node node, Field field)
    {
        String[] comment = this.comments.get(field);
        if (comment == null)
        {
            if (field.isAnnotationPresent(Comment.class))
            {
                comment = field.getAnnotation(Comment.class).value();
            }
            else
            {
                comment = NO_COMMENT;
            }
            this.comments.put(field, comment);
        }
        if (comment.length != 0)
        {
            node.setComments(comment);
        }
    }

    private void handleException(Exception e, Section section, Field field)
    {
        if (e instanceof InvalidReflectedObjectException)
        {
            throw (InvalidReflectedObjectException)e;
        }
        else if (e instanceof IllegalAccessException)
        {
            throw FieldAccessException.of(getPathFor(field), section.getClass(), field, e);
        }
        else if (e instanceof ConversionException)
        {
            // fatal ConversionException
            throw InvalidReflectedObjectException.of("Could not convert Field into Node!", getPathFor(field),
                                                     section.getClass(), field, e);
        }
        else
        {
            throw InvalidReflectedObjectException.of("Unknown Error while converting Section!", getPathFor(field),
                                                     section.getClass(), field, e);
        }
    }

    /**
     * Fills given section with the values from the node
     *
     * @param section the section to fill
     * @param node    the node
     * @param manager the manager
     */
    @SuppressWarnings("unchecked")
    public void fromNode(Section section, MapNode node, MapNode defaultNode,
                         ConverterManager manager) throws ConversionException
    {
        for (Field field : this.getReflectedFields(section.getClass()))
        {
            try
            {
                ReflectedPath fieldPath = getPathFor(field);
                Node fieldNode = node.getNodeAt(fieldPath);
                if (fieldNode instanceof ErrorNode)
                {
                    throw ConversionException.of(this, node, ((ErrorNode)fieldNode).getErrorMessage());
                }
                Object value;
                if (fieldNode instanceof NullNode)
                {
                    if (manager.getReflected().isChild())
                    {
                        manager.getReflected().addInheritedField(field);
                        fieldNode = defaultNode.getNodeAt(fieldPath);
                    }
                    else
                    {
                        manager.getReflected().getLogger().log(FINE, fieldPath + " is NULL! Ignoring missing value");
                        continue; // Take existing field Value
                    }
                }

                if (Section.class.isAssignableFrom(field.getType()))
                {
                    Section fillSection = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(),
                                                                            section);
                    manager.convertFromNode(node, fillSection);
                    value = fillSection;
                }
                else
                {
                    value = manager.convertFromNode(fieldNode, field.getGenericType());
                }
                field.set(section, value);
            }
            catch (Exception e)
            {
                this.handleException(e, section, field);
            }
        }
    }

    /**
     * Returns the fields to Reflect for given section
     *
     * @param clazz the sections class
     *
     * @return the fields to reflect
     */
    public final Field[] getReflectedFields(Class<? extends Section> clazz)
    {
        Field[] fields = this.cachedFields.get(clazz);
        if (fields != null)
        {
            return fields;
        }

        List<Field> list = new ArrayList<Field>();
        Set<String> paths = new HashSet<String>();

        Class<?> current = clazz;
        while (current != null)
        {
            for (Field field : current.getDeclaredFields())
            {
                if (!isReflectedField(field))
                {
                    continue;
                }

                if (!paths.add(getPathFor(field).toString()))
                {
                    throw new IllegalStateException("Duplicated Path detected! " + getPathFor(field)); // TODO exception
                }
                if (!field.isAccessible())
                {
                    field.setAccessible(true);
                }
                list.add(field);
            }
            current = current.getSuperclass();
        }

        fields = list.toArray(new Field[list.size()]);
        this.cachedFields.put(clazz, fields);
        return fields;
    }
}
