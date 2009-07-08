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
package org.apache.commons.net.ssh.keyprovider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.apache.commons.net.ssh.NamedFactory;
import org.apache.commons.net.ssh.PasswordFinder;
import org.apache.commons.net.ssh.PasswordFinder.Resource;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class PKCS8KeyFile implements FileKeyProvider
{
    
    public static class Factory implements NamedFactory<FileKeyProvider>
    {
        public FileKeyProvider create()
        {
            return new PKCS8KeyFile();
        }
        
        public String getName()
        {
            return "PKCS8";
        }
    }
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected PasswordFinder pwdf;
    protected String location;
    protected Resource resource;
    protected KeyPair kp;
    
    protected KeyType type;
    
    public PrivateKey getPrivate() throws IOException
    {
        return kp != null ? kp.getPrivate() : (kp = readKeyPair()).getPrivate();
    }
    
    public PublicKey getPublic() throws IOException
    {
        return kp != null ? kp.getPublic() : (kp = readKeyPair()).getPublic();
    }
    
    public KeyType getType() throws IOException
    {
        return type != null ? type : (type = KeyType.fromKey(getPublic()));
    }
    
    public void init(String location)
    {
        assert location != null;
        this.location = location;
        resource = new Resource(Resource.Type.KEYFILE, location);
    }
    
    public void init(String location, PasswordFinder pwdf)
    {
        init(location);
        this.pwdf = pwdf;
    }
    
    protected org.bouncycastle.openssl.PasswordFinder makeBouncyPasswordFinder()
    {
        if (pwdf == null)
            return null;
        else
            return new org.bouncycastle.openssl.PasswordFinder()
                {
                    
                    public char[] getPassword()
                    {
                        return pwdf.reqPassword(resource);
                    }
                };
    }
    
    protected KeyPair readKeyPair() throws IOException
    {
        KeyPair kp = null;
        org.bouncycastle.openssl.PasswordFinder pFinder = makeBouncyPasswordFinder();
        PEMReader r = null;
        Object o = null;
        for (;;) {
            try {
                r = new PEMReader(new InputStreamReader(new FileInputStream(location)), pFinder);
                o = r.readObject();
            } catch (IOException e) {
                if (e.toString().contains("javax.crypto.BadPaddingException")) // screen-scraping sucks
                    if (pwdf.retry(resource))
                        continue;
                throw e;
            } finally {
                try {
                    if (r != null)
                        r.close();
                } catch (IOException ignored) {
                }
            }
            break;
        }
        if (o instanceof KeyPair)
            kp = (KeyPair) o;
        else
            log.debug("Expected KeyPair, got {}", o.getClass().toString());
        return kp;
    }
}