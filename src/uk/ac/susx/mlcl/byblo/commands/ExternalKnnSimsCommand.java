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
package uk.ac.susx.mlcl.byblo.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Objects.ToStringHelper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;
import javax.naming.OperationNotSupportedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.susx.mlcl.byblo.enumerators.EnumeratingDeligates;
import uk.ac.susx.mlcl.byblo.enumerators.SingleEnumerating;
import uk.ac.susx.mlcl.byblo.io.KFirstReducerSink;
import uk.ac.susx.mlcl.byblo.io.TokenPair;
import uk.ac.susx.mlcl.byblo.io.Weighted;
import uk.ac.susx.mlcl.lib.Comparators;
import uk.ac.susx.mlcl.lib.tasks.Chunk;
import uk.ac.susx.mlcl.lib.tasks.ObjectMergeTask;
import uk.ac.susx.mlcl.lib.tasks.ObjectSortTask;

/**
 *
 * @author Hamish Morgan &lt;hamish.morgan@sussex.ac.uk%gt;
 */
@Parameters(commandDescription = "Perform k-nearest-neighbours on a similarity file.")
public class ExternalKnnSimsCommand extends ExternalSortEventsCommand {

    private static final Log LOG = LogFactory.getLog(
            ExternalKnnSimsCommand.class);

    public static final int DEFAULT_K = 100;

    private static final long serialVersionUID = 1L;

    @Parameter(names = {"-k"},
               description = "The number of neighbours to produce for each base entry.")
    private int k = DEFAULT_K;

    private Comparator<Weighted<TokenPair>> classComparator =
            Weighted.recordOrder(TokenPair.firstIndexOrder());

    private Comparator<Weighted<TokenPair>> nearnessComparator =
            Comparators.reverse(Weighted.<TokenPair>weightOrder());

    public ExternalKnnSimsCommand(
            File sourceFile, File destinationFile, Charset charset,
            SingleEnumerating indexDeligate,
            int k, int maxChunkSize) throws IOException {
        super(sourceFile, destinationFile, charset, EnumeratingDeligates.toPair(
                indexDeligate));
        setMaxChunkSize(maxChunkSize);
        setK(k);
    }

    public ExternalKnnSimsCommand(
            File sourceFile, File destinationFile, Charset charset,
            SingleEnumerating indexDeligate,
            int k) throws IOException {
        super(sourceFile, destinationFile, charset, EnumeratingDeligates.toPair(
                indexDeligate));
        setK(k);
    }

    public ExternalKnnSimsCommand() {
        super();
    }

    @Override
    @Deprecated
    public void setComparator(Comparator<Weighted<TokenPair>> comparator) {
        throw new RuntimeException(new OperationNotSupportedException());
    }

    @Override
    @Deprecated
    public Comparator<Weighted<TokenPair>> getComparator() {
        return getCombinedComparator();
    }

    public Comparator<Weighted<TokenPair>> getCombinedComparator() {
        return Comparators.fallback(
                getClassComparator(),
                getNearnessComparator());
    }

    public Comparator<Weighted<TokenPair>> getClassComparator() {
        return classComparator;
    }

    public void setClassComparator(
            Comparator<Weighted<TokenPair>> classComparator) {
        this.classComparator = classComparator;
    }

    public Comparator<Weighted<TokenPair>> getNearnessComparator() {
        return nearnessComparator;
    }

    public void setNearnessComparator(
            Comparator<Weighted<TokenPair>> nearnessComparator) {
        this.nearnessComparator = nearnessComparator;
    }

    public final int getK() {
        return k;
    }

    public final void setK(int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k < 1");
        }
        this.k = k;
    }

    @Override
    protected void initialiseTask() throws Exception {
        super.initialiseTask();
    }

    @Override
    protected void finaliseTask() throws Exception {
        super.finaliseTask();
    }

    @Override
    protected ObjectMergeTask<Weighted<TokenPair>> createMergeTask(File srcA,
                                                                   File srcB,
                                                                   File dst) throws IOException {
        ObjectMergeTask<Weighted<TokenPair>> task = super.createMergeTask(srcA,
                                                                          srcB,
                                                                          dst);
        task.setSink(new KFirstReducerSink<Weighted<TokenPair>>(
                task.getSink(), getClassComparator(), getK()));
        return task;
    }

    @Override
    protected ObjectSortTask<Weighted<TokenPair>> createSortTask(
            Chunk<Weighted<TokenPair>> chunk, File dst) throws IOException {
        ObjectSortTask<Weighted<TokenPair>> task = super.createSortTask(chunk,
                                                                        dst);
        task.setSink(new KFirstReducerSink<Weighted<TokenPair>>(
                task.getSink(), getClassComparator(), getK()));
        return task;
    }

    @Override
    protected ToStringHelper toStringHelper() {
        return super.toStringHelper().add("k", k);
    }
}
