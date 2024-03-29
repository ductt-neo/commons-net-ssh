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
package org.apache.commons.net.ssh;

import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * An interface for classes to which packet handling may be delegated. Chains of such delegations
 * may be used, e.g. {@code packet decoder -> (PacketHandler) transport layer -> (PacketHandler)
 * connection layer -> (PacketHandler) channel}.
 */
public interface PacketHandler
{
    /**
     * Delegate handling of some SSH packet to this object.
     * 
     * @param msg
     *            the SSH {@link Message message identifier}
     * @param buf
     *            {@link SSHPacket} containing rest of the request
     * @throws SSHException
     *             if there is a non-recoverable error
     */
    void handle(Message msg, SSHPacket buf) throws SSHException;
}
