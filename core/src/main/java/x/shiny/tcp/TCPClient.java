/*
 * Copyright 2019 guohaoice@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package x.shiny.tcp;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import com.google.protobuf.Service;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import x.shiny.Endpoint;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.handler.MessageHandler;
import x.shiny.handler.PipelineBuilder;
import x.shiny.invocation.Pipeline;
import x.shiny.protocol.ShinyProtocol;
import x.shiny.transport.RemoteInboundHandler;
import x.shiny.transport.RemoteOutboundHandler;

/**
 * @author guohaoice@gmail.com
 */
public class TCPClient {
    private static final EventLoopGroup WORKER;

    static {
        if (Epoll.isAvailable()) {
            // WORKER thread num = 2 * core num
            WORKER = new EpollEventLoopGroup(0, new DefaultThreadFactory("shiny-WORKER"));
            // WORKER thread may do some biz jobs , 50% ratio is rational.
        } else {
            WORKER = new NioEventLoopGroup(0, new DefaultThreadFactory("shiny-WORKER"));
        }
    }

    private final RemoteOutboundHandler handler = new RemoteOutboundHandler(this);
    @Getter
    private final Bootstrap bootstrap;
    @Getter
    private final Protocol protocol;
    private final List<Service> services;
    private final Endpoint endpoint;
    @Getter
    private Channel channel;

    public TCPClient(Endpoint endpoint) {
        this(endpoint, null, null);
    }

    public TCPClient(Endpoint endpoint, List<Service> services) {
        this(endpoint, null, services);
    }


    public TCPClient(Endpoint endpoint, Protocol protocol, List<Service> services) {
        this.endpoint = endpoint;
        if (protocol == null) {
            ShinyProtocol shinyProtocol = new ShinyProtocol();
            shinyProtocol.setServiceList(services);
            this.protocol = shinyProtocol;
        } else {
            this.protocol = protocol;
        }
        this.services = services;
        this.bootstrap = new Bootstrap();
    }

    public Future<Response> invoke(Request request) {
        return handler.invoke(request);
    }

    public void connect() {

        if (Epoll.isAvailable()) {
            // WORKER thread may do some biz jobs , 50% ratio is rational.
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }

        Pipeline requestPipeline = PipelineBuilder.buildRequestPipeline(services);
        RemoteInboundHandler inboundHandler = new RemoteInboundHandler(requestPipeline);
        final MessageHandler handler = new MessageHandler(Collections.singletonList(protocol), inboundHandler);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new IdleStateHandler(30, 30, 30));
                ch.pipeline().addLast(handler);
            }
        });

        bootstrap.group(WORKER);

        this.channel = bootstrap.connect(endpoint.host(), endpoint.port())
                .syncUninterruptibly()
                .channel();
    }
}
