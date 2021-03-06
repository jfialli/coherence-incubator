/*
 * File: MessagingMBeanProxy.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.coherence.patterns.messaging.management;

/**
 * This class acts as a MBean proxy to a cache object.  The {@link MessagingMBeanProxy} holds a reference
 * to the cache object which is used by sub-classes to provide MBean access.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Paul Mackin
 */
public class MessagingMBeanProxy
{
    /**
     * The managed cache object.
     */
    private Object object;


    /**
     * Set the managed cache object.
     *
     * @param object object to be managed
     */
    public void setObject(Object object)
    {
        this.object = object;
    }


    /**
     * Get the managed cache object.
     *
     * @return object
     */
    Object getObject()
    {
        return object;
    }
}
