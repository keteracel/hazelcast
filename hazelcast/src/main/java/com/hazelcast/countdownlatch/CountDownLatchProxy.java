/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.countdownlatch;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.MemberLeftException;
import com.hazelcast.monitor.LocalCountDownLatchStats;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.Invocation;
import com.hazelcast.spi.NodeEngine;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @mdogan 1/10/13
 */
public class CountDownLatchProxy extends AbstractDistributedObject implements ICountDownLatch, DistributedObject {

    private final String name;
    private final int partitionId;

    public CountDownLatchProxy(String name, NodeEngine nodeEngine) {
        super(nodeEngine);
        this.name = name;
        partitionId = nodeEngine.getPartitionService().getPartitionId(nodeEngine.toData(name));
    }

    public String getName() {
        return name;
    }

//    public void await() throws MemberLeftException, InterruptedException {
//        awaitInternal(-1L);
//    }

    public boolean await(long timeout, TimeUnit unit) throws MemberLeftException, InterruptedException {
        return awaitInternal(unit.toMillis(timeout));
    }

    private boolean awaitInternal(long timeout) throws MemberLeftException, InterruptedException {
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new AwaitOperation(name, timeout), partitionId).build();
        try {
            return (Boolean) nodeEngine.toObject(inv.invoke().get());
        } catch (ExecutionException e) {
            throw new HazelcastException(e);
        }
    }

    public void countDown() {
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new CountDownOperation(name), partitionId).build();
        try {
            inv.invoke().get();
        } catch (Exception e) {
            throw new HazelcastException(e);
        }
    }

    public int getCount() {
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new GetCountOperation(name), partitionId).build();
        try {
            return (Integer) nodeEngine.toObject(inv.invoke().get());
        } catch (Exception e) {
            throw new HazelcastException(e);
        }
    }

    public boolean trySetCount(int count) {
        Invocation inv = nodeEngine.getOperationService().createInvocationBuilder(CountDownLatchService.SERVICE_NAME,
                new SetCountOperation(name, count), partitionId).build();
        try {
            return (Boolean) nodeEngine.toObject(inv.invoke().get());
        } catch (Exception e) {
            throw new HazelcastException(e);
        }
    }

    public LocalCountDownLatchStats getLocalCountDownLatchStats() {
        return null;
    }

    @Override
    protected String getServiceName() {
        return CountDownLatchService.SERVICE_NAME;
    }

    public Object getId() {
        return name;
    }
}
