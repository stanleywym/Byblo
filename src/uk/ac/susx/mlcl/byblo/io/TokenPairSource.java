/*
 * Copyright (c) 2010-2012, University of Sussex
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

import uk.ac.susx.mlcl.byblo.enumerators.EnumeratingDeligates;
import uk.ac.susx.mlcl.byblo.enumerators.DoubleEnumerating;
import uk.ac.susx.mlcl.byblo.enumerators.SingleEnumeratingDeligate;
import com.google.common.base.Predicate;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.ac.susx.mlcl.byblo.enumerators.*;
import uk.ac.susx.mlcl.lib.io.*;

/**
 * An <tt>TokenPairSource</tt> object is used to retrieve
 * {@link EntryFeature} objects from a flat file.
 *
 * @author Hamish Morgan &lt;hamish.morgan@sussex.ac.uk&gt;
 * @see EntryFeatureSink
 */
public class TokenPairSource
        implements SeekableSource<TokenPair, Tell>, Closeable {

    private final SeekableDataSource inner;

    public TokenPairSource(SeekableDataSource inner)
            throws FileNotFoundException, IOException {
        this.inner = inner;
    }

    @Override
    public TokenPair read() throws IOException {
        try {
            final int id1 = inner.readInt();
            final int id2 = inner.readInt();
            inner.endOfRecord();
            return new TokenPair(id1, id2);
        } catch (Throwable ex) {
            throw new IOException("Error at position " + position(), ex);
        }
    }

    public static boolean equal(File fileA, File fileB, Charset charset, boolean skip1, boolean skip2)
            throws IOException {
        DoubleEnumerating idx = EnumeratingDeligates.toPair(
                new SingleEnumeratingDeligate(Enumerating.DEFAULT_TYPE, false,
                                              null));
        final TokenPairSource srcA = open(fileA, charset, idx, skip1, skip2);
        final TokenPairSource srcB = open(fileB, charset, idx, skip1, skip2);


        List<TokenPair> listA = IOUtil.readAll(srcA);
        List<TokenPair> listB = IOUtil.readAll(srcB);
        Comparator<TokenPair> c = TokenPair.indexOrder();
        Collections.sort(listA, c);
        Collections.sort(listB, c);
        return listA.equals(listB);
    }

    @Override
    public boolean hasNext() throws IOException {
        return inner.canRead();
    }

    @Override
    public void position(Tell p) throws IOException {
        inner.position(p);
    }

    @Override
    public Tell position() throws IOException {
        return inner.position();
    }

    @Override
    public void close() throws IOException {
        if (inner instanceof Closeable)
            ((Closeable) inner).close();
    }

    public static TokenPairSource open(
            File file, Charset charset, DoubleEnumerating idx, boolean skip1, boolean skip2)
            throws IOException {
        SeekableDataSource tsv = new TSV.Source(file, charset);

        if (skip1) {
            tsv = Deltas.deltaInt(tsv, new Predicate<Integer>() {

                @Override
                public boolean apply(Integer column) {
                    return column == 0;
                }

            });
        }
        if (skip2) {
            tsv = Deltas.deltaInt(tsv, new Predicate<Integer>() {

                @Override
                public boolean apply(Integer column) {
                    return column > 0;
                }

            });
        }

        tsv = Compact.compact(tsv, 2);

        if (!idx.isEnumeratedEntries() || !idx.isEnumeratedFeatures()) {
            @SuppressWarnings("unchecked")
            Enumerator<String>[] enumerators = (Enumerator<String>[]) new Enumerator[2];
            if (!idx.isEnumeratedEntries())
                enumerators[0] = idx.getEntryEnumerator();
            if (!idx.isEnumeratedFeatures())
                enumerators[1] = idx.getFeatureEnumerator();
            tsv = Enumerated.enumerated(tsv, enumerators);
        }
        return new TokenPairSource(tsv);
    }

}
