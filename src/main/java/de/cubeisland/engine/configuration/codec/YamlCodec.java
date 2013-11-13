/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.cubeisland.engine.configuration.codec;

import de.cubeisland.engine.configuration.Configuration;
import de.cubeisland.engine.configuration.StringUtils;
import de.cubeisland.engine.configuration.exception.InvalidConfigurationException;
import de.cubeisland.engine.configuration.node.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;

import static de.cubeisland.engine.configuration.StringUtils.isEmpty;

/**
 * A Codec for YAML-Configurations allowing child-configurations
 */
public class YamlCodec extends ConfigurationCodec
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

    // Configuration loading Method

    @Override
    @SuppressWarnings("unchecked")
    protected final MapNode load(InputStream is, Configuration config) throws InvalidConfigurationException
    {
        MapNode values;
        try
        {
            if (is == null)
            {
                values = MapNode.emptyMap(); // InputStream null -> config was not existent
                return values;
            }
            Map<Object, Object> map = (Map<Object, Object>)new Yaml().load(is);
            if (map == null)
            {
                values = MapNode.emptyMap(); // loadValues null -> config exists but was empty
            }
            else
            {
                values = (MapNode) CONVERTERS.wrapIntoNode(map);
            }
        }
        catch (ScannerException ex)
        {
            throw new InvalidConfigurationException("Failed to parse the YAML configuration. Try encoding it as UTF-8 or validate on yamllint.com", ex);
        }
        return values;
    }

    // Configuration saving Methods

    @Override
    protected final void save(MapNode node, OutputStream os, Configuration config) throws IOException
    {
        try
        {
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
            if (config.head() != null)
            {
                writer.append("# ").append(StringUtils.implode("\n# ", config.head())).append(LINE_BREAK).append(LINE_BREAK);
            }
            convertMapNode(writer, node, 0, false);
            if (config.tail() != null)
            {
                writer.append("# ").append(StringUtils.implode("\n# ", config.tail()));
            }
            writer.flush();
            writer.close();
        }
        finally
        {
            os.close();
        }
    }

    /**
     * Serializes a single <code>Node</code> that is NOT a <code>ParentNode</code>
     *
     * @param writer the OutputStreamWriter to serialize into
     * @param value the Node to serialize
     * @param offset the current offset
     * @throws IOException
     */
    private void convertValue(OutputStreamWriter writer, Node value, int offset) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        if (!(value instanceof NullNode)) // null-Node ?
        {
            if (value instanceof StringNode) // String-Node ?
            {
                String string = ((StringNode)value).getValue();
                if (string.contains(LINE_BREAK)) // MultiLine String
                {
                    String offsetString = getOffset(offset);
                    sb.append("|").append(LINE_BREAK).append(offsetString).append(OFFSET);
                    sb.append(string.trim().replace(LINE_BREAK, LINE_BREAK + offsetString + OFFSET));
                }
                else if (needsQuote(string))
                {
                    sb.append(QUOTE).append(string).append(QUOTE);
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
     * @param value the MapNode to serialize
     * @param offset the current offset
     * @param inList true if currently directly under a ListNode
     * @throws IOException
     */
    private void convertMapNode(OutputStreamWriter writer, MapNode value, int offset, boolean inList) throws IOException
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
            if (!isEmpty(comment))
            {
                if (!hasLine && !first) // if not already one line free
                {
                    sb.append(LINE_BREAK); // add free line before comment
                }
                sb.append(comment);
            }

            if (!(first && inList))
            {
                sb.append(getOffset(offset)); // Map in collection first does not get offset
            }
            sb.append(value.getOriginalKey(entry.getKey())).append(": ");
            writer.append(sb.toString());
            // Now convert the value
            if (entry.getValue() instanceof MapNode) // Map-Node?
            {
                if (((MapNode)entry.getValue()).isEmpty())
                {
                    writer.append("{}");
                }
                else
                {
                    writer.append(LINE_BREAK);
                    convertMapNode(writer, ((MapNode)entry.getValue()), offset + 1, false);
                }
                endOfMapOrList = true;
            }
            else if (entry.getValue() instanceof ListNode) // List-Node? -> list the nodes
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
            else // Other Node (list / normal)
            {
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
     * @param value the ListNode to serialize
     * @param offset the current offset
     * @throws IOException
     */
    private void convertListNode(OutputStreamWriter writer, ListNode value, int offset) throws IOException
    {
        writer.append(LINE_BREAK);
        boolean endOfMapOrList = false;
        for (Node listedNode : value.getListedNodes()) //Convert Collection
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
                convertListNode(writer, (ListNode)listedNode, offset);
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
     * @param offset the current offset
     * @return the built comment
     */
    private String buildComment(String[] comments, int offset)
    {
        if (comments == null || comments.length == 0)
        {
            return ""; //No Comment
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
                comment = comment.replace(LINE_BREAK, LINE_BREAK + off + COMMENT_PREFIX); // multi line
            }
            sb.append(off).append(COMMENT_PREFIX).append(comment).append(LINE_BREAK);
        }
        return sb.toString();
    }

    /**
     * Returns whether a string needs to be quoted in YAML
     *
     * @param s the string to check
     * @return true if the given string needs quoting
     */
    private boolean needsQuote(String s)
    {
        return (s.startsWith("#") || s.contains(" #") || s.startsWith("@") || s.startsWith("`") || s.startsWith("[") || s.startsWith("]") || s.startsWith("{") || s.startsWith("}") || s.startsWith("|") || s
                .startsWith(">") || s.startsWith("!") || s.startsWith("%") || s.endsWith(":") || s.startsWith("- ") || s.startsWith(",") || s.contains("&") || s.matches("[0-9]+:[0-9]+")) || isEmpty(s) || s
                .equals("*") || s.matches("[0][0-9]+");
    }
}
