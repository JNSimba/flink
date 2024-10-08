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

package org.apache.flink.runtime.webmonitor;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.rest.handler.LeaderRetrievalHandler;
import org.apache.flink.runtime.rest.handler.router.RoutedRequest;
import org.apache.flink.runtime.rest.handler.router.Router;
import org.apache.flink.runtime.rest.handler.util.HandlerRedirectUtils;
import org.apache.flink.runtime.rest.handler.util.KeepAliveWrite;
import org.apache.flink.runtime.webmonitor.retriever.GatewayRetriever;
import org.apache.flink.runtime.webmonitor.testutils.HttpTestClient;
import org.apache.flink.runtime.webmonitor.utils.WebFrontendBootstrap;

import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponse;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.http.HttpResponseStatus;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests for the {@link LeaderRetrievalHandler}. */
class LeaderRetrievalHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderRetrievalHandlerTest.class);

    private static final String RESPONSE_MESSAGE = "foobar";

    /**
     * Tests the behaviour of the LeaderRetrievalHandler under the following conditions.
     *
     * <p>1. No gateway resolved --> service unavailable 2. leader gateway
     *
     * @throws Exception
     */
    @Test
    void testLeaderRetrievalGateway() throws Exception {
        final String restPath = "/testing";

        final Configuration configuration = new Configuration();
        final Router router = new Router();
        final Duration timeout = Duration.ofSeconds(10L);
        final CompletableFuture<RestfulGateway> gatewayFuture = new CompletableFuture<>();
        final GatewayRetriever<RestfulGateway> gatewayRetriever = () -> gatewayFuture;
        final RestfulGateway gateway = new TestingRestfulGateway.Builder().build();

        final TestingHandler testingHandler = new TestingHandler(gatewayRetriever, timeout);

        router.addGet(restPath, testingHandler);
        WebFrontendBootstrap bootstrap =
                new WebFrontendBootstrap(router, LOG, null, null, "localhost", 0, configuration);

        try (HttpTestClient httpClient =
                new HttpTestClient("localhost", bootstrap.getServerPort())) {
            // 1. no leader gateway available --> Service unavailable
            httpClient.sendGetRequest(restPath, timeout);

            HttpTestClient.SimpleHttpResponse response = httpClient.getNextResponse(timeout);

            assertThat(response.getStatus()).isEqualTo(HttpResponseStatus.SERVICE_UNAVAILABLE);

            // 2. with leader
            gatewayFuture.complete(gateway);

            httpClient.sendGetRequest(restPath, timeout);

            response = httpClient.getNextResponse(timeout);

            assertThat(response.getStatus()).isEqualTo(HttpResponseStatus.OK);
            assertThat(response.getContent()).isEqualTo(RESPONSE_MESSAGE);

        } finally {
            bootstrap.shutdown();
        }
    }

    private static class TestingHandler extends LeaderRetrievalHandler<RestfulGateway> {

        protected TestingHandler(
                @Nonnull GatewayRetriever<RestfulGateway> leaderRetriever,
                @Nonnull Duration timeout) {
            super(leaderRetriever, timeout, Collections.emptyMap());
        }

        @Override
        protected void respondAsLeader(
                ChannelHandlerContext channelHandlerContext,
                RoutedRequest routedRequest,
                RestfulGateway gateway)
                throws Exception {
            assertThat(channelHandlerContext.channel().eventLoop().inEventLoop()).isTrue();
            HttpResponse response =
                    HandlerRedirectUtils.getResponse(HttpResponseStatus.OK, RESPONSE_MESSAGE);
            KeepAliveWrite.flush(
                    channelHandlerContext.channel(), routedRequest.getRequest(), response);
        }
    }
}
