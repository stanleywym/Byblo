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
package uk.ac.susx.mlcl.byblo.tasks;

import uk.ac.susx.mlcl.lib.tasks.FileDeleteTask;
import com.beust.jcommander.JCommander;
import java.util.ResourceBundle;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Hamish Morgan &lt;hamish.morgan@sussex.ac.uk&gt;
 */
public class DeleteTaskTest {

 
    @Test(timeout=1000)
    public void testRun_success() throws Exception {
        System.out.println("Testing run() -- expecting success");
        File tmp = File.createTempFile(getClass().getName(), "");
        assertTrue(tmp.exists());
        FileDeleteTask instance = new FileDeleteTask(tmp);
        instance.run();
        while(instance.isExceptionCaught())
            instance.throwException();
        assertFalse(tmp.exists());
    }

    @Test(expected = IOException.class, timeout=1000)
    public void testRunTask_failure() throws Exception {
        System.out.println("Testing runTask() -- expecting failure");
        File tmp = File.createTempFile(getClass().getName(), "");
        tmp.delete();
        assertFalse(tmp.exists());
        FileDeleteTask instance = new FileDeleteTask(tmp);
        instance.run();
        instance.throwException();
    }

    @Test(expected = IOException.class,timeout=1000)
    public void testRun_failure() throws Exception {
        System.out.println("Testing runTask() -- expecting failure");
        File tmp = File.createTempFile(getClass().getName(), "");
        tmp.delete();
        assertFalse(tmp.exists());
        FileDeleteTask instance = new FileDeleteTask(tmp);
        instance.run();
        instance.throwException();
    }

    @Test(timeout=1000)
    public void testGetSetFile() throws IOException {
        System.out.println("Testing getFile() and setFile()");
        File tmp = File.createTempFile(getClass().getName(), "");
        FileDeleteTask instance = new FileDeleteTask(tmp);
        File expResult = tmp;
        assertEquals(expResult, instance.getFile());
        instance = new FileDeleteTask();
        instance.setFile(tmp);
        assertEquals(tmp, instance.getFile());
        tmp.delete();
    }

}
