/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.state.forst;

import org.apache.flink.annotation.Internal;

import java.util.concurrent.CompletableFuture;

/**
 * Data access operation to ForStDB. This interface is used to encapsulate the DB access operations
 * formed after grouping state access. For more information about “Grouping state access”, please
 * refer to FLIP-426.
 */
@Internal
public interface ForStDBOperation {

    /**
     * Process the ForStDB access requests.
     *
     * @return The future which indicates whether the operation is completed.
     */
    CompletableFuture<Void> process();

    /**
     * The count of sub-processes. Each sub-process is an atomic operation in one single thread.
     *
     * @return the count.
     */
    int subProcessCount();
}
