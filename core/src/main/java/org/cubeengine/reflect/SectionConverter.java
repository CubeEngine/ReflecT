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
package org.cubeengine.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.converter.ClassedConverter;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.NullNode;
import org.cubeengine.converter.node.Path;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.exception.DuplicatedPathException;
import org.cubeengine.reflect.exception.FieldAccessException;
import org.cubeengine.reflect.exception.InvalidReflectedObjectException;
import org.cubeengine.reflect.util.SectionFactory;
import org.cubeengine.reflect.util.StringUtils;

import static org.cubeengine.reflect.Reflector.LOGGER;
import static org.cubeengine.reflect.annotations.Name.SEPARATOR;
import static java.util.logging.Level.FINE;

/**
 * A converter for Sections.
 * <p>
 * This converter will cache the Fields of Sections to speed up repeated saving and loading of the same section
 */
public class SectionConverter implements ClassedConverter<Section>
{
    private static final String[] NO_COMMENT = new String[0];
    private final Map<Field, Path> paths = new HashMap<Field, Path>();
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
    protected final Path getPathFor(Field field)
    {
        Path path = this.paths.get(field);
        if (path == null)
        {
            if (field.isAnnotationPresent(Name.class))
            {
                path = new Path(SEPARATOR, field.getAnnotation(Name.class).value());
            }
            else
            {
                path = new Path(SEPARATOR, StringUtils.fieldNameToPath(field.getName())); // TODO configurable Naming convention #20
            }
            this.paths.put(field, path);
        }
        return path;
    }

    public Node toNode(Section section, ConverterManager manager) throws ConversionException
    {
        if (!(manager instanceof ReflectedConverterManager))
        {
            throw new IllegalArgumentException("provided ConverterManager is not a ReflectedConverterManager");
        }
        ReflectedConverterManager rManager = (ReflectedConverterManager)manager;

        MapNode baseNode = MapNode.emptyMap();
        Class<? extends Section> sectionClass = section.getClass();

        for (Field field : this.getReflectedFields(sectionClass))
        {
            if (rManager.getReflected().isInheritedField(field))
            {
                continue; // do not save inherited field of child config
            }
            try
            {
                Node newNode = toNode(section, rManager, field);
                addComment(newNode, field);

                Node prevNode = baseNode.get(getPathFor(field));
                if (prevNode instanceof MapNode)
                {
                    if (newNode instanceof MapNode)
                    {
                        for (Entry<String, Node> entry : ((MapNode)newNode).getMappedNodes().entrySet())
                        {
                            ((MapNode)prevNode).set(entry.getKey(), entry.getValue());
                        }
                    }
                }
                else
                {
                    baseNode.set(getPathFor(field), newNode);
                }
            }
            catch (Exception e)
            {
                this.handleException(e, section, field);
            }
        }
        if (rManager.getReflected().isChild())
        {
            // remove generated empty ParentNodes ONLY from child-reflected
            baseNode.cleanUpEmptyNodes();
        }
        return baseNode;
    }

    @SuppressWarnings("unchecked")
    private Node toNode(Section section, ConverterManager manager, Field field) throws ConversionException, IllegalAccessException
    {
        Node newNode;
        if (field.isAnnotationPresent(org.cubeengine.reflect.annotations.Converter.class))
        {
            newNode = manager.getConverterByClass(field.getAnnotation(
                org.cubeengine.reflect.annotations.Converter.class).value()).toNode(field.get(section), manager);
        }
        else
        {
            newNode = manager.convertToNode(field.get(section));
        }
        return newNode;
    }

    /**
     * Adds a comment to the given Node
     *
     * @param node  the Node to add the comment to
     * @param field the field possibly having a {@link org.cubeengine.reflect.annotations.Comment} annotation
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

    @SuppressWarnings("unchecked")
    public Section fromNode(Node aNode, Class<? extends Section> clazz, ConverterManager manager) throws ConversionException
    {
        if (!(manager instanceof ReflectedConverterManager))
        {
            throw new IllegalArgumentException("provided ConverterManager is not a ReflectedConverterManager");
        }
        ReflectedConverterManager rManager = (ReflectedConverterManager)manager;

        Section section = SectionFactory.newSectionInstance(clazz, null);
        MapNode mapNode = (MapNode)aNode;

        for (Field field : this.getReflectedFields(clazz))
        {
            try
            {
                Path fieldPath = getPathFor(field);
                Node fieldNode = mapNode.get(fieldPath);
                Object value;
                if (fieldNode == null || fieldNode instanceof NullNode)
                {
                    LOGGER.log(FINE, fieldPath + " is NULL! Ignoring missing value");
                    continue; // Take existing field Value
                }

                if (fieldNode.isInherited())
                {
                    rManager.getReflected().addInheritedField(field);
                }

                if (field.isAnnotationPresent(org.cubeengine.reflect.annotations.Converter.class))
                {
                    value = rManager.getConverterByClass(field.getAnnotation(
                        org.cubeengine.reflect.annotations.Converter.class).value()).fromNode(fieldNode, field.getType(), rManager);
                }
                else
                {
                    value = rManager.convertFromNode(fieldNode, field.getGenericType());
                }
                field.set(section, value);
            }
            catch (Exception e)
            {
                this.handleException(e, section, field);
            }
        }
        return section;
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
        Set<String> resolvedPaths = new HashSet<String>();

        Class<?> current = clazz;
        while (current != null)
        {
            for (Field field : current.getDeclaredFields())
            {
                if (!isReflectedField(field))
                {
                    continue;
                }

                if (!resolvedPaths.add(getPathFor(field).asString(SEPARATOR)))
                {
                    throw new DuplicatedPathException("Duplicated Path detected! " + getPathFor(field));
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
