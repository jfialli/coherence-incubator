/*
 * File: AbstractInterClusterEventChannelBuilder.java
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

package com.oracle.coherence.patterns.eventdistribution.channels;

import com.oracle.coherence.common.cluster.ClusterMetaInfo;

import com.oracle.coherence.common.events.Event;

import com.oracle.coherence.configuration.Property;

import com.oracle.coherence.patterns.eventdistribution.EventChannelEventFilter;
import com.oracle.coherence.patterns.eventdistribution.channels.InterClusterEventChannel.DistributionRole;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.logging.Level;

/**
 * An {@link AbstractInterClusterEventChannelBuilder} is a base builder implementation for
 * {@link InterClusterEventChannel}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
@SuppressWarnings("serial")
public abstract class AbstractInterClusterEventChannelBuilder extends AbstractEventChannelBuilder
    implements EventChannelEventFilter
{
    /**
     *  The {@link DistributionRole} for the constructed {@link InterClusterEventChannel}s.
     */
    private DistributionRole distributionRole = DistributionRole.LEAF;


    /**
     * Required for {@link ExternalizableLite} and {@link PortableObject}.
     */
    public AbstractInterClusterEventChannelBuilder()
    {
        super();
    }


    /**
     * Sets the {@link DistributionRole} for the {@link com.oracle.coherence.patterns.eventdistribution.EventChannel}s
     * realized by this {@link com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder}.
     *
     * @param distributionRole  the {@link DistributionRole}
     */
    @Property("distribution-role")
    public void setDistributionRole(DistributionRole distributionRole)
    {
        this.distributionRole = distributionRole;
    }


    /**
     * Obtains the {@link DistributionRole} for the {@link com.oracle.coherence.patterns.eventdistribution.EventChannel}s
     * realized by this {@link com.oracle.coherence.patterns.eventdistribution.EventChannelBuilder}.
     *
     * @return the {@link DistributionRole}
     */
    public DistributionRole getDistributionRole()
    {
        return distributionRole;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept(Event           event,
                          ClusterMetaInfo source,
                          ClusterMetaInfo local)
    {
        // determine if we should distribute
        boolean isLocal = local.equals(source);

        if (isLocal)
        {
            // we always accept local events for distribution
            return true;
        }
        else
        {
            // we only accept non-local events for distribution when we're not a LEAF
            return getDistributionRole() != InterClusterEventChannel.DistributionRole.LEAF;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in) throws IOException
    {
        super.readExternal(in);
        this.distributionRole = DistributionRole.valueOf(ExternalizableHelper.readSafeUTF(in));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out) throws IOException
    {
        super.writeExternal(out);
        ExternalizableHelper.writeSafeUTF(out, distributionRole.toString());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader reader) throws IOException
    {
        super.readExternal(reader);
        this.distributionRole = DistributionRole.valueOf(reader.readString(101));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter writer) throws IOException
    {
        super.writeExternal(writer);
        writer.writeString(101, distributionRole.toString());
    }
}
