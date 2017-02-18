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
package de.cubeisland.engine.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import de.cubeisland.engine.reflect.ReflectedTestFile.SubSection2.SubSubSection;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;

public class ReflectedTestFile extends ReflectedFile
{
    @Comment("First Comment! [report here]")
    @Name("subsection-using.annotation.first")
    public String s1;

    @Comment("This is a comment\nwith multiple\nlines using\\n")
    public String subsectionUsing_annotation_string;

    @Comment({"This is a multi-line comment too", "but using the array"})
    @Name("subsection-using.annotation.quoted")
    public String s3;

    public Level level;

    @Name("subsection-using.annotation.unquoted")
    public String s4;

    @Comment("This comments a section")
    @Name("subsection-using.section")
    public SubSection subSection;

    @Comment("Set of SubSections:")
    public Set<SubSection> subsections;

    public class SubSection implements Section
    {
        @Comment("This is a comment on a field in a sub-section")
        public boolean bool;
        public int integer;
        public String multilineString;
    }

    @Comment("All of these shall need quotes!")
    public QuotedStrings quotedStrings;

    public class QuotedStrings implements Section
    {
        public String s1;
        public String s2;
        public String s3;
        public String s4;
        public String s5;
        public String s6;
        public String s7;
        public String s8;
        public String s9;
        public String s10;
        public String s11;
        public String s12;
        public String s13;
    }

    @Comment("Testing Collections & Arrays")
    public CollectionsStuff collections;

    public class CollectionsStuff implements Section
    {
        public List<List<Double>> doubleListInList;

        public String[] stringArray;

        public LinkedList<String> stringList;
        public List<Short> shortList;

        @Comment("map in collection")
        public Collection<Map<String, String>> mapInCollection;
    }

    @Comment("Testing Maps")
    public Maps maps;

    public class Maps implements Section
    {
        public HashMap<String, Integer> map1;
        @Comment("multimapinmap")
        public LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Integer>>> mapinmapinmap;
    }

    public StaticSection staticSection;

    public static class StaticSection implements Section
    {
        public boolean bool;
    }

    public ExternalSection externalSection;

    public SubSection2 subsection;

    public class SubSection2 implements Section
    {
        public Set<SubSubSection> subsubsections;

        public class SubSubSection implements Section
        {
            public String something = "something else";
        }
    }

    public Map<String, SubSection> mappedSections;

    private SubSection getDefaultSubSection()
    {
        SubSection subSection = new SubSection();

        subSection.bool = true;
        subSection.integer = 123456;
        subSection.multilineString = "This string has\nmultiple lines";

        return subSection;
    }

    private QuotedStrings getDefaultQuotedStrings()
    {
        QuotedStrings quotedStrings = new QuotedStrings();

        quotedStrings.s1 = "#Not A Comment";
        quotedStrings.s2 = "Important non comment stuff: # 42!";
        quotedStrings.s3 = "@Comment is used to add a comment to any field in a reflected-section";
        quotedStrings.s4 = "{This is not a map}";
        quotedStrings.s5 = "%s <- replace that now";
        quotedStrings.s6 = "not followed by a map:";
        quotedStrings.s7 = "!take care!";
        quotedStrings.s8 = "& now?";
        quotedStrings.s9 = "0123456789";
        quotedStrings.s10 = "";
        quotedStrings.s11 = "*";
        quotedStrings.s12 = "123:456";
        quotedStrings.s13 = "'Quoted' Not Quoted";

        return quotedStrings;
    }

    private CollectionsStuff getDefaultCollectionStuff()
    {
        CollectionsStuff collectionsStuff = new CollectionsStuff();

        collectionsStuff.doubleListInList = new ArrayList<List<Double>>()
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
        collectionsStuff.stringArray = new String[]{
            "text1", "text2", "text3"
        };
        collectionsStuff.stringList = new LinkedList<String>()
        {
            {
                add("string1");
                add("string2");
            }
        };
        collectionsStuff.shortList = new LinkedList<Short>()
        {
            {
                short s = 123;
                add(s);
                s = 124;
                add(s);
            }
        };

        Map<String, String> map = new HashMap<String, String>();
        map.put("abc", "123");
        map.put("def", "456");
        collectionsStuff.mapInCollection = new ArrayList<Map<String, String>>();
        collectionsStuff.mapInCollection.add(map);
        map = new HashMap<String, String>();
        map.put("ghi", "789");
        map.put("jkl", "012");
        collectionsStuff.mapInCollection.add(map);

        return collectionsStuff;
    }

    private Maps getDefaultMaps()
    {
        Maps maps = new Maps();

        maps.map1 = new HashMap<String, Integer>()
        {
            {
                put("default", 7);
            }
        };
        maps.mapinmapinmap = new LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Integer>>>()
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

        return maps;
    }

    private StaticSection getDefaultStaticSection()
    {
        StaticSection staticSection = new StaticSection();

        staticSection.bool = false;

        return staticSection;
    }

    private SubSection2 getDefaultSubSection2()
    {
        SubSection2 subSection2 = new SubSection2();

        subSection2.subsubsections = new HashSet<SubSubSection>();

        return subSection2;
    }

    public static ReflectedTestFile getDefaultReflectedTest(Reflector reflector)
    {
        final ReflectedTestFile reflectedTest = reflector.create(ReflectedTestFile.class);

        reflectedTest.s1 = "Using @Name(\"subsection-using.annotation.first\") Annotation for path";
        reflectedTest.subsectionUsing_annotation_string = "Using fieldName = subsectionUsing_annotation_string for path";
        reflectedTest.s3 = "|This will be quoted";
        reflectedTest.level = Level.INFO;
        reflectedTest.s4 = "This needs no quotes";
        reflectedTest.subSection = reflectedTest.getDefaultSubSection();

        reflectedTest.subsections = new HashSet<SubSection>();
        reflectedTest.subsections.add(reflectedTest.getDefaultSubSection());

        reflectedTest.quotedStrings = reflectedTest.getDefaultQuotedStrings();
        reflectedTest.collections = reflectedTest.getDefaultCollectionStuff();
        reflectedTest.maps = reflectedTest.getDefaultMaps();
        reflectedTest.staticSection = reflectedTest.getDefaultStaticSection();
        reflectedTest.externalSection = new ExternalSection();
        reflectedTest.subsection = reflectedTest.getDefaultSubSection2();
        reflectedTest.mappedSections = new HashMap<String, SubSection>()
        {
            {
                put("key", reflectedTest.getDefaultSubSection());
            }
        };

        return reflectedTest;
    }
}
