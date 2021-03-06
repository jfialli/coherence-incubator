/*
 * File: TransitionTrackingAction.java
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

package com.oracle.coherence.common.finitestatemachines;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A Tracking Action for {@link TransitionAction}s.
 * <p>
 * Copyright (c) 2013. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @param <S>  the type of the state
 *
 * @author Brian Oliver
 */
public class TransitionTrackingAction<S extends Enum<S>> implements TransitionAction<S>
{
    /**
     * The number of times the action has been executed.
     */
    private AtomicLong m_executionCount;


    /**
     * Constructs a {@link TransitionTrackingAction}.
     */
    public TransitionTrackingAction()
    {
        m_executionCount = new AtomicLong(0);
    }


    /**
     * Obtains the number of times the action has been executed.
     *
     * @return the number of times the action has been executed
     */
    public long getExecutionCount()
    {
        return m_executionCount.get();
    }


    /**
     * {@inheritDoc}
     */
    public void onTransition(String           sName,
                             S                stateFrom,
                             S                stateTo,
                             Event<S>         event,
                             ExecutionContext context) throws RollbackTransitionException
    {
        m_executionCount.incrementAndGet();
    }
}
