/*
 * Copyright (c) 2010-2011, University of Sussex
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Sussex nor the names of its 
 *    contributors may be used to endorse or promote products derived from this 
 *    software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package uk.ac.susx.mlcl.byblo.io;

import uk.ac.susx.mlcl.lib.ObjectIndex;
import uk.ac.susx.mlcl.lib.io.AbstractTSVSource;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.susx.mlcl.lib.io.Lexer.Tell;

/**
 * An <tt>EntrySource</tt> object is used to retrieve {@link EntryRecord} 
 * objects from a flat file. 
 * 
 * @author Hamish Morgan &lt;hamish.morgan@sussex.ac.uk&gt;
 * @see EntrySink
 */
public class EntrySource extends AbstractTSVSource<EntryRecord> {

    private static final Logger LOG = Logger.getLogger(
            EntrySource.class.getName());

    private final ObjectIndex<String> stringIndex;

    private double weightSum = 0;

    private double weightMax = 0;

    private int widthSum = 0;

    private int widthMax = 0;

    private int cardinality = 0;

    private int count = 0;

    private EntryRecord previousRecord = null;

    public EntrySource(File file, Charset charset,
            ObjectIndex<String> stringIndex)
            throws FileNotFoundException, IOException {
        super(file, charset);
        if (stringIndex == null)
            throw new NullPointerException("stringIndex == null");
        this.stringIndex = stringIndex;
    }

    public EntrySource(File file, Charset charset)
            throws FileNotFoundException, IOException {
        this(file, charset, new ObjectIndex<String>());
    }

    public ObjectIndex<String> getStringIndex() {
        return stringIndex;
    }

    public double getWeightSum() {
        return weightSum;
    }

    public double getWeightMax() {
        return weightMax;
    }

    public int getWidthMax() {
        return widthMax;
    }

    public int getWidthSum() {
        return widthSum;
    }

    public int getCardinality() {
        return cardinality;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void position(Tell offset) throws IOException {
        super.position(offset);
        previousRecord = null;
    }

    @Override
    public Tell position() {
        return super.position();
    }

    @Override
    public EntryRecord read() throws IOException {
        final int entryId;
        if (previousRecord == null) {
            entryId = stringIndex.get(parseString());
            parseValueDelimiter();
        } else {
            entryId = previousRecord.getEntryId();
        }

        if (!hasNext() || isDelimiterNext()) {
            parseRecordDelimiter();
            throw new SingletonRecordException(this,
                    "Found entry record with no weights.");
        }

        final double weight = parseDouble();

        cardinality = Math.max(cardinality, entryId + 1);
        weightSum += weight;
        weightMax = Math.max(weightMax, weight);
        ++count;
        final EntryRecord record = new EntryRecord(entryId, weight);

        if (isValueDelimiterNext()) {
            parseValueDelimiter();
            previousRecord = record;
        }

        if (hasNext() && isRecordDelimiterNext()) {
            parseRecordDelimiter();
            previousRecord = null;
        }

        return record;
    }

    public Int2DoubleMap readAll() throws IOException {
        Int2DoubleMap entityFrequenciesMap = new Int2DoubleOpenHashMap();
        while (this.hasNext()) {
            EntryRecord entry = this.read();
            if (entityFrequenciesMap.containsKey(entry.getEntryId())) {
                // If we encounter the same EntryRecord more than once, it means
                // the perl has decided two strings are not-equal, which java
                // thinks are equal ... so we need to merge the records:

                // TODO: Not true any more.. remove this code?

                final int id = entry.getEntryId();
                final double oldFreq = entityFrequenciesMap.get(id);
                final double newFreq = oldFreq + entry.getWeight();

                LOG.log(Level.WARNING,
                        "Found duplicate Entry \"{0}\" (id={1}) in entries "
                        + "file. Merging records. Old frequency = {2}, new "
                        + "frequency = {3}.",
                        new Object[]{stringIndex.get(entry.getEntryId()), id, oldFreq, newFreq});

                entityFrequenciesMap.put(id, newFreq);
            }
            entityFrequenciesMap.put(entry.getEntryId(), entry.getWeight());
        }
        return entityFrequenciesMap;
    }

    public double[] readAllAsArray() throws IOException {
        Int2DoubleMap tmp = readAll();
        double[] entryFreqs = new double[getCardinality()];
        ObjectIterator<Int2DoubleMap.Entry> it = tmp.int2DoubleEntrySet().
                iterator();
        while (it.hasNext()) {
            Int2DoubleMap.Entry entry = it.next();
            entryFreqs[entry.getIntKey()] = entry.getDoubleValue();
        }
        return entryFreqs;
    }

    public static boolean equal(File a, File b, Charset charset) throws IOException {
        final ObjectIndex<String> stringIndex = new ObjectIndex<String>();
        final EntrySource srcA = new EntrySource(a, charset, stringIndex);
        final EntrySource srcB = new EntrySource(b, charset, stringIndex);
        boolean equal = true;
        while (equal && srcA.hasNext() && srcB.hasNext()) {
            final EntryRecord recA = srcA.read();
            final EntryRecord recB = srcB.read();
            equal = recA.getEntryId() == recB.getEntryId()
                    && recA.getWeight() == recB.getWeight();
        }
        return equal && srcA.hasNext() == srcB.hasNext();
    }
}
