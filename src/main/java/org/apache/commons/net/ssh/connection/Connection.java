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
package org.apache.commons.net.ssh.connection;

import java.io.IOException;

import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.transport.Session;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

/*
 * STUB!
 * 
 */
public class Connection implements ConnectionService
{
    
    private final Session session;
    
    public Connection(Session session)
    {
        this.session = session;
    }
    
    public String getName()
    {
        return NAME;
    }
    
    public void handle(Message cmd, Buffer packet) throws SSHException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void request() throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void setError(Exception ex)
    {
        // TODO Auto-generated method stub
    }
    
}
