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

import java.util.List;
import java.util.concurrent.Future;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import x.shiny.Endpoint;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.channel.InvocationPipeline;
import x.shiny.channel.PipelineBuilder;
import x.shiny.handler.MessageHandler;
import x.shiny.handler.PacketProcessor;
import x.shiny.handler.ShinyPacketProcessor;

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

    @Getter
    private final Bootstrap bootstrap;
    private final List<Protocol> protocols;
    private final List<Object> services;
    private final Endpoint endpoint;
    private Channel channel;

    public TCPClient(Endpoint endpoint) {
        this(endpoint, null, null);
    }

    public TCPClient(Endpoint endpoint, List<Object> services) {
        this(endpoint, null, services);
    }


    public TCPClient(Endpoint endpoint, List<Protocol> protocols, List<Object> services) {
        this.endpoint = endpoint;
        this.protocols = protocols;
        this.services = services;
        this.bootstrap = new Bootstrap();
    }

    public Future<Response> invoke(Request request) {
        channel.write(request);
    }
    public void invoke(String interfaceName, String methodName, Object[] args, String[] argsType) {

    }

    public void connect() {

        if (Epoll.isAvailable()) {
            // WORKER thread may do some biz jobs , 50% ratio is rational.
            bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            bootstrap.channel(NioServerSocketChannel.class);
        }

        InvocationPipeline requestPipeline = PipelineBuilder.buildRequestPipeline(services);
        InvocationPipeline responsePipeline = PipelineBuilder.buildResponsePipeline();
        PacketProcessor processor = new ShinyPacketProcessor(requestPipeline, responsePipeline);
        final MessageHandler handler = new MessageHandler(protocols, processor);
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
