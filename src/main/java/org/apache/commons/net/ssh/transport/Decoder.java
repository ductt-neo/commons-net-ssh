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
package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.SSHPacket;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.util.BufferUtils;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes packets from the SSH binary protocol per the current algorithms.
 */
final class Decoder extends Converter
{
    
    private static final int MAX_PACKET_LEN = 256 * 1024;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /** What we pass decoded packets to */
    private final PacketHandler packetHandler;
    /** Buffer where as-yet undecoded data lives */
    private final SSHPacket inputBuffer = new SSHPacket();
    /** Used in case compression is active to store the uncompressed data */
    private final SSHPacket uncompressBuffer = new SSHPacket();
    /** MAC result is stored here */
    private byte[] macResult;
    
    /** -1 if packet length not yet been decoded, else the packet length */
    private int packetLength = -1;
    
    /**
     * How many bytes do we need, before a call to decode() can succeed at decoding at least packet length, OR the whole
     * packet?
     */
    private int needed = 8;
    
    Decoder(PacketHandler packetHandler)
    {
        this.packetHandler = packetHandler;
    }
    
    private void checkMAC(final byte[] data) throws TransportException
    {
        if (mac != null)
        {
            mac.update(seq); // seq num
            mac.update(data, 0, packetLength + 4); // packetLength+4 = entire packet w/o mac
            mac.doFinal(macResult, 0); // compute
            // Check against the received MAC
            if (!BufferUtils.equals(macResult, 0, data, packetLength + 4, mac.getBlockSize()))
                throw new TransportException(DisconnectReason.MAC_ERROR, "MAC Error");
        }
    }
    
    /**
     * Returns advised number of bytes that should be made available in decoderBuffer before the method should be called
     * again.
     * 
     * @return number of bytes needed before further decoding possible
     */
    private int decode() throws SSHException
    {
        int need;
        
        /* Decoding loop */
        for (;;)
            
            if (packetLength == -1) // Waiting for beginning of packet
            {
                
                assert inputBuffer.rpos() == 0 : "buffer cleared";
                
                need = cipherSize - inputBuffer.available();
                if (need <= 0)
                    packetLength = decryptLength();
                else
                    // Need more data
                    break;
                
            } else
            {
                
                assert inputBuffer.rpos() == 4 : "packet length read";
                
                need = packetLength + (mac != null ? mac.getBlockSize() : 0) - inputBuffer.available();
                if (need <= 0)
                {
                    
                    decryptPayload(inputBuffer.array());
                    
                    seq = seq + 1 & 0xffffffffL;
                    
                    checkMAC(inputBuffer.array());
                    
                    // Exclude the padding & MAC
                    inputBuffer.wpos(packetLength + 4 - inputBuffer.readByte());
                    
                    SSHPacket plain = decompressed();
                    
                    if (log.isTraceEnabled())
                        log.trace("Received packet #{}: {}", seq, plain.printHex());
                    
                    packetHandler.handle(plain.readMessageID(), plain); // Process the decoded packet //
                    
                    inputBuffer.clear();
                    packetLength = -1;
                    need = cipherSize;
                    
                } else
                    // Need more data
                    break;
            }
        
        return need;
    }
    
    private SSHPacket decompressed() throws TransportException
    {
        if (compression != null && (authed || !compression.isDelayed()))
        {
            uncompressBuffer.clear();
            compression.uncompress(inputBuffer, uncompressBuffer);
            return uncompressBuffer;
        } else
            return inputBuffer;
    }
    
    private int decryptLength() throws TransportException
    {
        cipher.update(inputBuffer.array(), 0, cipherSize);
        
        final int len = inputBuffer.readInt(); // Read packet length
        
        if (len < 5 || len > MAX_PACKET_LEN)
        { // Check packet length validity
            log.info("Error decoding packet (invalid length) {}", inputBuffer.printHex());
            throw new TransportException(DisconnectReason.PROTOCOL_ERROR, "invalid packet length: " + len);
        }
        
        return len;
    }
    
    private void decryptPayload(final byte[] data)
    {
        cipher.update(data, cipherSize, packetLength + 4 - cipherSize);
    }
    
    /**
     * Adds {@code len} bytes from {@code b} to the decoder buffer. When a packet has been successfully decoded, hooks
     * in to {@link PacketHandler#handle} of the {@link PacketHandler} this decoder was initialized with.
     * <p>
     * Returns the number of bytes expected in the next call in order to decode the packet length, and if the packet
     * length has already been decoded; to decode the payload. This number is accurate and should be taken to heart.
     */
    int received(byte[] b, int len) throws SSHException
    {
        inputBuffer.putRawBytes(b, 0, len);
        if (needed <= len)
            needed = decode();
        else
            needed -= len;
        return needed;
    }
    
    @Override
    void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        macResult = new byte[mac.getBlockSize()];
        if (compression != null)
            compression.init(Compression.Type.Inflater, -1);
    }
    
    int getMaxPacketLength()
    {
        return MAX_PACKET_LEN;
    }
    
}