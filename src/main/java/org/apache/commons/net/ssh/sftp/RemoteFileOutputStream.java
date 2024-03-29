/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.io.OutputStream;

public class RemoteFileOutputStream extends OutputStream
{
    
    private final RemoteFile rf;
    
    private final byte[] b = new byte[1];
    
    private long fileOffset;
    
    public RemoteFileOutputStream(RemoteFile rf)
    {
        this(rf, 0);
    }
    
    public RemoteFileOutputStream(RemoteFile rf, long fileOffset)
    {
        this.rf = rf;
        this.fileOffset = fileOffset;
    }
    
    @Override
    public void write(int w) throws IOException
    {
        b[0] = (byte) w;
        write(b, 0, 1);
    }
    
    @Override
    public void write(byte[] buf, int off, int len) throws IOException
    {
        rf.write(fileOffset, buf, off, len);
        fileOffset += len;
    }
    
}
