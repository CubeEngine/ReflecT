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
package de.cubeisland.engine.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.TestCodec;

/**
 * A Reflected implementation for unit test
 */
public class ReflectedTest extends ReflectedFile<TestCodec>
{
    @Comment("First Comment! [report here]")
    @Name("subsection-using.annotation.first")
    public String s1 = "Using @Name(\"subsection-using.annotation.first\") Annotation for path";

    @Comment("This is a comment\nwith multiple\nlines using\\n")
    public String subsectionUsing_annotation_string = "Using fieldName = subsectionUsing_annotation_string for path";

    @Comment({"This is a multi-line comment too", "but using the array"})
    @Name("subsection-using.annotation.quoted")
    public String s3 = "|This will be quoted";

    public Level level = Level.INFO;

    @Name("subsection-using.annotation.unquoted")
    public String s4 = "This needs no quotes";

    @Comment("This comments a section")
    @Name("subsection-using.section")
    public SubSection section = new SubSection();

    public class SubSection implements Section
    {
        @Comment("This is a comment on a field in a sub-section")
        public boolean bool = true;
        public int integer = 123456;
        public String multilineString = "This string has\nmultiple lines";
    }

    @Comment("All of these shall need quotes!")
    public QuotedStrings quotedStrings = new QuotedStrings();

    public class QuotedStrings implements Section
    {
        public String s1 = "#Not A Comment";
        public String s2 = "Important non comment stuff: # 42!";
        public String s3 = "@Comment is used to add a comment to any field in a reflected-section";
        public String s4 = "{This is not a map}";
        public String s5 = "%s <- replace that now";
        public String s6 = "not followed by a map:";
        public String s7 = "!take care!";
        public String s8 = "& now?";
        public String s9 = "0123456789";
        public String s10 = "";
        public String s11 = "*";
        public String s12 = "123:456";
    }

    @Comment("Testing Collections & Arrays")
    public CollectionsStuff collections = new CollectionsStuff();

    public class CollectionsStuff implements Section
    {
        public List<List<Double>> doubleListInList = new ArrayList<List<Double>>()
        {
            {
                ArrayList<Double> doubles = new ArrayList<Double>();
                add(doubles);
                doubles.add(0.0);
                doubles.add(1.0);
                doubles = new ArrayList<Double>();
                add(doubles);
                doubles.add(0.0);
                doubles.add(1.0);
            }
        };

        public String[] stringArray = {
            "text1", "text2", "text3"
        };

        public LinkedList<String> stringList = new LinkedList<String>()
        {
            {
                add("string1");
                add("string2");
            }
        };
        public List<Short> shortList = new LinkedList<Short>()
        {
            {
                short s = 123;
                add(s);
                s = 124;
                add(s);
            }
        };

        @Comment("map in collection")
        public Collection<Map<String, String>> mapInCollection;

        {
            Map<String, String> map = new HashMap<String, String>();
            map.put("abc", "123");
            map.put("def", "456");
            mapInCollection = new ArrayList<Map<String, String>>();
            mapInCollection.add(map);
            map = new HashMap<String, String>();
            map.put("ghi", "789");
            map.put("jkl", "012");
            mapInCollection.add(map);
        }
    }

    @Comment("Testing Maps")
    public Maps maps = new Maps();

    public class Maps implements Section
    {
        public HashMap<String, Integer> map1 = new HashMap<String, Integer>()
        {
            {
                put("default", 7);
            }
        };
        @Comment("multimapinmap")
        public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Integer>>> mapinmapinmap = new LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Integer>>>()
        {
            {
                LinkedHashMap<String, LinkedHashMap<String, Integer>> map1 = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
                LinkedHashMap<String, Integer> map2 = new LinkedHashMap<String, Integer>();
                map2.put("oneTwoThree", 123);
                map1.put("inmap", map2);
                map1.put("inmap2", new LinkedHashMap<String, Integer>());
                this.put("map", map1);
            }
        };
    }

    public StaticSection staticSection = new StaticSection();

    public static class StaticSection implements Section
    {
        public boolean bool = false;
    }

    public ExternalSection externalSection = new ExternalSection();
}
