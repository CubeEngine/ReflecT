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
package de.cubeisland.engine.configuration.node;

public class ErrorNode extends Node<Void>
{
    private final String message;
    private final Exception ex;

    public ErrorNode(String message)
    {
        this.message = message;
        this.ex = null;
    }

    public ErrorNode(Exception ex)
    {
        this.ex = ex;
        this.message = ex.getMessage();
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
        return "ErrorNode=[" + message + "]";
    }

    public Exception getExeption()
    {
        return ex;
    }
}
