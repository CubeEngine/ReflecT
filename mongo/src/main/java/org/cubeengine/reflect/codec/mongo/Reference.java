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
package org.cubeengine.reflect.codec.mongo;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import org.cubeengine.reflect.Reflector;

public class Reference<T extends ReflectedDBObject>
{
    private final Reflector reflector;
    private final DBCollection collection;
    private final DBObject object;
    private DBRef dbRef;
    private T fetched = null;

    public Reference(Reflector reflector, DBRef dbRefBase)
    {
        this.reflector = reflector;
        this.dbRef = dbRefBase;

        this.collection = null;
        this.object = null;
    }

    public Reference(Reflector reflector, DBCollection collection, DBObject object)
    {
        this.reflector = reflector;
        this.collection = collection;
        this.object = object;
    }

    public T fetch(Class<T> clazz)
    {
        if (fetched == null)
        {
            DBObject fetch = collection.findOne(this.dbRef.getId());
            if (fetch == null)
            {
                Reflector.LOGGER.warning("The DB Reference points to nothing: " + this.dbRef);
                return null;
            }
            this.fetched = this.reflector.load(clazz, collection.findOne(this.getDBRef().getId()));
        }
        return fetched;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Reference && this.dbRef.equals(((Reference)obj).dbRef);
    }

    @Override
    public int hashCode()
    {
        return this.dbRef.hashCode();
    }

    public DBRef getDBRef()
    {
        if (this.dbRef == null)
        {
            this.dbRef = new DBRef(collection.getName(), object.get("_id"));
        }
        return this.dbRef;
    }
}
