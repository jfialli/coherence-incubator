/*
 * File: CoherenceBasedPushReplicationTest.java
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

package com.oracle.coherence.patterns.pushreplication;

import com.oracle.coherence.patterns.eventdistribution.EventDistributor;

import com.oracle.tools.runtime.coherence.ClusterMemberSchema;

import com.oracle.tools.util.Capture;

import org.junit.Test;

/**
 * The {@link CoherenceBasedPushReplicationTest} is an {@link AbstractPushReplicationTest} designed
 * to test the Coherence-based {@link EventDistributor}.
 * <p>
 * Copyright (c) 2010. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public class CoherenceBasedPushReplicationTest extends AbstractPushReplicationTest
{
    /**
     * {@inheritDoc}
     */
    protected ClusterMemberSchema newBaseClusterMemberSchema(Capture<Integer> clusterPort)
    {
        return super.newBaseClusterMemberSchema(clusterPort).setSystemProperty("event.distributor.config",
                                                                               "test-coherence-based-distributor-config.xml")
                                                                                   .setSystemProperty("proxy.port",
                                                                                                      getAvailablePortIterator());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected ClusterMemberSchema newPassiveClusterMemberSchema(Capture<Integer> clusterPort)
    {
        return newBaseClusterMemberSchema(clusterPort).setCacheConfigURI("test-passive-cluster-cache-config.xml")
            .setClusterName("passive");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected ClusterMemberSchema newActiveClusterMemberSchema(Capture<Integer> clusterPort)
    {
        return newBaseClusterMemberSchema(clusterPort)
            .setCacheConfigURI("test-remotecluster-eventchannel-cache-config.xml").setClusterName("active");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testActivePassive() throws Exception
    {
        super.testActivePassive();
    }


    /**
     * {@inheritDoc}
     */
    @Test
    public void testActiveActiveConflictResolution() throws Exception
    {
        super.testActiveActiveConflictResolution();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testActivePassiveEventTransformation() throws Exception
    {
        super.testActivePassiveEventTransformation();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testActivePassiveEventFiltering() throws Exception
    {
        super.testActivePassiveEventFiltering();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testAsyncCacheStoreEventChannel() throws Exception
    {
        super.testAsyncCacheStoreEventChannel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void testAsyncPutAllWithCacheStoreChannel() throws Exception
    {
        super.testAsyncPutAllWithCacheStoreChannel();
    }
}
