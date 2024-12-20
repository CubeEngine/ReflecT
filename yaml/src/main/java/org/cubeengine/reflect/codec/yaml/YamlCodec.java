/*
 * The MIT License
 * Copyright © 2013 Cube Island
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
package org.cubeengine.reflect.codec.yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.NullNode;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.reflect.Reflected;
import org.cubeengine.reflect.ReflectedFile;
import org.cubeengine.reflect.codec.ReaderWriterFileCodec;
import org.cubeengine.reflect.util.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import static org.cubeengine.reflect.util.StringUtils.isEmpty;

/**
 * A Codec using the YAML format
 */
public class YamlCodec extends ReaderWriterFileCodec
{
    private static final String COMMENT_PREFIX = "# ";
    private static final String OFFSET = "  ";
    private static final String LINE_BREAK = "\n";
    private static final String QUOTE = "'";

    @Override
    public final String getExtension()
    {
        return "yml";
    }

    // Reflected loading Method
    @Override
    @SuppressWarnings("unchecked")
    protected final MapNode load(Reader in, Reflected reflected) throws ConversionException
    {
        try
        {
            if (in == null)
            {
                // InputStream null -> reflected was not existent
                return MapNode.emptyMap();
            }
            Map<Object, Object> map = (Map<Object, Object>)new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
            if (map == null)
            {
                // loadValues null -> reflected exists but was empty
                return MapNode.emptyMap();
            }
            return (MapNode)this.getConverterManager().convertToNode(map);
        }
        catch (ScannerException ex)
        {
            throw ConversionException.of(this, in, "Failed to parse the YAML reflected object. Try encoding it as UTF-8 or validate on yamllint.com", ex);
        }
        catch (ParserException ex)
        {
            throw ConversionException.of(this, in, "Failed to parse the YAML reflected object. Try encoding it as UTF-8 or validate on yamllint.com", ex);
        }
    }

    // Reflected saving Methods
    @Override
    protected final void save(MapNode node, Writer writer, Reflected reflected) throws ConversionException
    {
        try
        {
            ReflectedFile fRef = null;
            if (reflected instanceof ReflectedFile)
            {
                fRef = (ReflectedFile)reflected;
            }
            if (fRef != null && fRef.head() != null && fRef.head().length != 0)
            {
                writer.append("# ").append(StringUtils.implode("\n# ", fRef.head())).append(LINE_BREAK)
                      .append(LINE_BREAK);
            }
            convertMapNode(writer, node, 0, false);
            if (fRef != null && fRef.tail() != null && fRef.tail().length != 0)
            {
                writer.append("# ").append(StringUtils.implode("\n# ", fRef.tail()));
            }
        }
        catch (IOException ex)
        {
            throw ConversionException.of(this, null, "Could not write into OutputStream", ex);
        }
    }

    /**
     * Serializes a single <code>Node</code> that is NOT a <code>ParentNode</code>
     *
     * @param writer the OutputStreamWriter to serialize into
     * @param value  the Node to serialize
     * @param offset the current offset
     */
    private void convertValue(Writer writer, Node value, int offset) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        // null-Node ?
        if (!(value instanceof NullNode))
        {
            // String-Node ?
            if (value instanceof StringNode)
            {
                String string = ((StringNode)value).getValue();
                // MultiLine String
                if (string.contains(LINE_BREAK))
                {
                    String offsetString = getOffset(offset);
                    sb.append("|").append(LINE_BREAK).append(offsetString).append(OFFSET);
                    sb.append(string.trim().replace(LINE_BREAK, LINE_BREAK + offsetString + OFFSET));
                }
                else if (needsQuote(string))
                {
                    sb.append(QUOTE).append(string.replace(QUOTE, QUOTE + QUOTE)).append(QUOTE);
                }
                else
                {
                    sb.append(string);
                }
                writer.append(sb.toString());
            }
            else
            {
                writer.append(value.asText());
            }
        }
        writer.append(LINE_BREAK);
    }

    /**
     * Serializes the values in the <code>MapNode</code>
     *
     * @param writer the OutputStreamWriter to serialize into
     * @param value  the MapNode to serialize
     * @param offset the current offset
     * @param inList true if currently directly under a ListNode
     */
    private void convertMapNode(Writer writer, MapNode value, int offset, boolean inList) throws IOException
    {
        Map<String, Node> map = value.getMappedNodes();
        boolean endOfMapOrList = false;
        boolean first = true;
        for (Entry<String, Node> entry : map.entrySet())
        {
            boolean hasLine = false;
            if (endOfMapOrList && !inList)
            {
                writer.append(LINE_BREAK);
                hasLine = true;
            }
            StringBuilder sb = new StringBuilder();
            String comment = buildComment(entry.getValue().getComments(), offset);
            if (!isEmpty(comment.trim()))
            {
                // if not already one line free
                if ((!hasLine && !first) || inList)
                {
                    sb.append(LINE_BREAK);
                    // add free line before comment
                }
                sb.append(comment);
            }

            if (!(first && inList) || (!comment.isEmpty()))
            {
                // Map in collection first does not get offset
                sb.append(getOffset(offset));
            }
            sb.append(value.getOriginalKey(entry.getKey())).append(": ");
            writer.append(sb.toString());
            // Now convert the value
            if (entry.getValue() instanceof MapNode)
            {
                if (((MapNode)entry.getValue()).isEmpty())
                {
                    writer.append("{}");
                }
                else
                {
                    writer.append(LINE_BREAK);
                    convertMapNode(writer, (MapNode)entry.getValue(), offset + 1, false);
                }
                endOfMapOrList = true;
            }
            else if (entry.getValue() instanceof ListNode)
            {
                if (((ListNode)entry.getValue()).isEmpty())
                {
                    writer.append("[]").append(LINE_BREAK);
                }
                else
                {
                    convertListNode(writer, (ListNode)entry.getValue(), offset);
                }
                endOfMapOrList = true;
            }
            else
            {
                // Other Node (list / normal)
                convertValue(writer, entry.getValue(), offset);
                endOfMapOrList = false;
            }
            first = false;
        }
    }

    /**
     * Serializes the values in the <code>ListNode</code>
     *
     * @param writer the OutputStreamWriter to serialize into
     * @param value  the ListNode to serialize
     * @param offset the current offset
     */
    private void convertListNode(Writer writer, ListNode value, int offset) throws IOException
    {
        writer.append(LINE_BREAK);
        boolean endOfMapOrList = false;
        //Convert Collection
        for (Node listedNode : value.getValue())
        {
            if (endOfMapOrList)
            {
                writer.append(LINE_BREAK);
            }
            writer.append(getOffset(offset)).append(OFFSET).append("- ");
            if (listedNode instanceof MapNode)
            {
                if (((MapNode)listedNode).isEmpty())
                {
                    writer.append("{}");
                }
                else
                {
                    convertMapNode(writer, (MapNode)listedNode, offset + 2, true);
                }
                endOfMapOrList = true;
            }
            else if (listedNode instanceof ListNode)
            {
                convertListNode(writer, (ListNode)listedNode, offset + 1);
                endOfMapOrList = true;
            }
            else
            {
                convertValue(writer, listedNode, offset + 1);
                endOfMapOrList = false;
            }
        }
    }

    // HELPER Methods

    /**
     * Returns the offset as String
     *
     * @param offset the offset
     *
     * @return the offset
     */
    private String getOffset(int offset)
    {
        StringBuilder off = new StringBuilder("");
        for (int i = 0; i < offset; ++i)
        {
            off.append(OFFSET);
        }
        return off.toString();
    }

    /**
     * Builds the a comment
     *
     * @param comments the comment-lines
     * @param offset   the current offset
     *
     * @return the built comment
     */
    private String buildComment(String[] comments, int offset)
    {
        if (comments == null || comments.length == 0)
        {
            //No Comment
            return "";
        }
        String off = getOffset(offset);
        StringBuilder sb = new StringBuilder();
        for (String comment : comments)
        {
            if (isEmpty(comment))
            {
                continue;
            }
            if (comment.contains("\n"))
            {
                // multi line
                comment = comment.replace(LINE_BREAK, LINE_BREAK + off + COMMENT_PREFIX);
            }
            sb.append(off).append(COMMENT_PREFIX).append(comment).append(LINE_BREAK);
        }
        return sb.toString();
    }

    /**
     * Returns whether a string needs to be quoted in YAML
     *
     * @param s the string to check
     *
     * @return true if the given string needs quoting
     */
    private boolean needsQuote(String s)
    {
        return (s.startsWith("#") || s.contains(" #") || s.startsWith("@") || s.startsWith("`")
             || s.startsWith("[") || s.startsWith("]") || s.startsWith("{") || s.startsWith("}")
             || s.startsWith("|") || s.startsWith(">") || s.startsWith("!") || s.startsWith("%")
             || s.endsWith(":") || s.contains(": ") || s.startsWith("- ") || s.startsWith(",")
             || s.contains("&") || s.matches("[0-9]+:[0-9]+")) || isEmpty(s) || "*".equals(s)
             || s.startsWith(" ") || s.contains(QUOTE) || s.matches("[0][0-9]+");
    }
}
