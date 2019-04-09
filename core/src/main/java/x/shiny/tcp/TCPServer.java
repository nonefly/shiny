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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import x.shiny.Protocol;
import x.shiny.channel.InvocationPipeline;
import x.shiny.channel.PipelineBuilder;
import x.shiny.handler.MessageHandler;
import x.shiny.handler.PacketProcessor;
import x.shiny.handler.ShinyPacketProcessor;
import x.shiny.protocol.ShinyProtocol;
import x.shiny.util.NetElf;

/**
 * @author guohaoice@gmail.com
 */
@Slf4j
public class TCPServer {

    @Getter
    private final ServerBootstrap bootstrap;

    private final List<Protocol> protocols;

    private final List<Object> services;

    public TCPServer(List<Object> services) {
        this(NetElf.randomPort(), null, services);
    }

    public TCPServer(int port) {
        this(port, null, null);
    }

    public TCPServer(int port, List<Protocol> protocols) {
        this(port, protocols, null);
    }

    public TCPServer(int port, List<Protocol> protocols, List<Object> services) {
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.localAddress(port);
        if (services != null) {
            this.services = services;
        } else {
            this.services = new ArrayList<>();
        }
        if (protocols == null) {
            List<ShinyProtocol> defaultProtocol = Collections.singletonList(new ShinyProtocol());
            this.protocols = Collections.unmodifiableList(defaultProtocol);
        } else {
            this.protocols = Collections.unmodifiableList(protocols);
        }
    }

    public void start() {
        EventLoopGroup boss;
        EventLoopGroup worker;

        if (Epoll.isAvailable()) {
            boss = new EpollEventLoopGroup(1, new DefaultThreadFactory("shiny-boss"));
            // worker thread num = 2 * core num
            worker = new EpollEventLoopGroup(0, new DefaultThreadFactory("shiny-worker"));
            // boss thread will do nothing except IO events.
            ((EpollEventLoopGroup) boss).setIoRatio(100);
            // worker thread may do some biz jobs , 50% ratio is rational.
            bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            boss = new NioEventLoopGroup(1, new DefaultThreadFactory("shiny-boss"));
            worker = new NioEventLoopGroup(0, new DefaultThreadFactory("shiny-worker"));
            ((NioEventLoopGroup) boss).setIoRatio(100);
            bootstrap.channel(NioServerSocketChannel.class);
        }

        InvocationPipeline requestPipeline = PipelineBuilder.buildRequestPipeline(services);
        InvocationPipeline responsePipeline = PipelineBuilder.buildResponsePipeline();
        PacketProcessor processor = new ShinyPacketProcessor(requestPipeline, responsePipeline);
        final MessageHandler handler = new MessageHandler(protocols, processor);
        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new IdleStateHandler(60, 60, 60));
                pipeline.addLast(handler);
            }
        };
        bootstrap.group(boss, worker).childHandler(initializer);

        try {
            ChannelFuture f = bootstrap.bind();
            f.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    if (log.isInfoEnabled()) {
                        log.info("server started on {} success", future.channel().localAddress());
                    }
                }
            });
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
