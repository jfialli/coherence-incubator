/*
 * File: EventChannelControllerMBean.java
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

package com.oracle.coherence.patterns.eventdistribution;

import com.oracle.coherence.common.events.Event;

import com.tangosol.net.NamedCache;

/**
 * The {@link EventChannelControllerMBean} specifies the JMX interface for {@link EventChannelController}s.
 * <p>
 * Copyright (c) 2011. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public interface EventChannelControllerMBean
{
    /**
     * Returns the name of the {@link EventDistributor} for the {@link EventChannelController}.
     *
     * @return A {@link String}.
     */
    public String getEventDistributorName();


    /**
     * Returns the {@link EventChannelController.Identifier} for the {@link EventChannelController}.
     *
     * @return A {@link String}
     */
    public String getEventChannelControllerName();


    /**
     * Returns the state of the {@link EventChannelController}.
     */
    public String getEventChannelControllerState();


    /**
     * Returns the duration (in ms) of the last distributed batch.
     */
    public long getLastBatchDistributionDuration();


    /**
     * Returns the minimum distribution duration (in ms).
     */
    public long getMinimumBatchDistributionDuration();


    /**
     * Returns the maximum distribution duration (is ms).
     */
    public long getMaximumBatchDistributionDuration();


    /**
     * Returns the total duration (in ms) the spend distributing {@link Event}s using the {@link EventChannel}.
     */
    public long getTotalDistributionDuration();


    /**
     * Returns the current number of consecutive {@link EventChannel} failures.
     */
    public int getConsecutiveEventChannelFailures();


    /**
     * Returns the total number of batches of {@link Event}s distributed.
     */

    public int getEventBatchesDistributedCount();


    /**
     * Returns the total number of {@link Event}s seen and considered for distribution.
     */

    public int getCandidateEventCount();


    /**
     * Returns the total number of {@link Event} distributed.
     */

    public int getEventsDistributedCount();


    /**
     * Determines the number of milliseconds the {@link EventChannelController} should wait when checking for
     * new {@link Event}s to distribute.
     *
     * @return time (in milliseconds).
     */
    public long getBatchDistributionDelay();


    /**
     * Sets the number of milliseconds the {@link EventChannelController} should wait when checking for
     * new {@link Event}s to distribute.
     *
     * @param delayMS  time in milliseconds
     */
    public void setBatchDistributionDelay(long delayMS);


    /**
     * Determines the maximum number of {@link Event}s to distribute in a single batch using an {@link EventChannel}.
     *
     * @return An {@link Integer}.
     */
    public int getBatchSize();


    /**
     * Sets the maximum number of {@link Event}s to distribute in a single batch using an {@link EventChannel}.
     *
     * @param batchSize  the maximum number of {@link Event}s to distribute in a batch
     */
    public void setBatchSize(int batchSize);


    /**
     * Determines the number of milliseconds the {@link EventChannelController} should delay between distribution
     * failures.
     *
     * @return time (in milliseconds)
     */
    public long getRestartDelay();


    /**
     * Sets the number of milliseconds the {@link EventChannelController} should delay between distribution
     * failures. (0 means no delay)
     *
     * @param delayMS  time in milliseconds
     */
    public void setRestartDelay(long delayMS);


    /**
     * Suspends the {@link EventChannelController} for distribution.  While suspended the {@link EventChannelController}
     * will queue up all requests to distribute {@link Event}s.  When {@link #start()}ed the
     * {@link EventChannelController} will commence distributing the queued {@link Event}s.
     */
    public void suspend();


    /**
     * Schedules the {@link EventChannelController} commence distribution of {@link Event}s.
     * (if it was Suspended or Disabled).
     */
    public void start();


    /**
     * Schedules the {@link EventChannelController} to disable distribution.  While disabled the
     * {@link EventChannelController} will ignore all request to distribute {@link Event}s by its associated
     * {@link EventDistributor}.
     */
    public void disable();


    /**
     * Discards all of the currently queued {@link Event}s for distribution.  The {@link EventChannelController}
     * must be <strong>suspended</strong> before this happens.
     */
    public void drain();


    /**
     * Propagates a copy of an underlying {@link NamedCache} as a set of propagation {@link Event}s over an
     * {@link EventChannel}.  The {@link EventChannelController} must either be <strong>suspended</strong> or
     * <strong>disabled</strong> to allow propagation to commence.
     */
    public void propagate();
}
