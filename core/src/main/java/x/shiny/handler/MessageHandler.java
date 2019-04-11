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

package x.shiny.handler;

import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import x.shiny.Packet;
import x.shiny.Protocol;
import x.shiny.channel.Session;
import x.shiny.channel.ShinySession;
import x.shiny.common.BadDataFormatException;
import x.shiny.transport.RemoteInboundHandler;

/**
 * Bi-direction is supported, so there is no different handlers between server and client.
 *
 * @author guohaoice@gmail.com
 */
@Slf4j
@AllArgsConstructor
@ChannelHandler.Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {
    private final List<Protocol> protocols;
    private final RemoteInboundHandler handler;


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) {
        if (event instanceof IdleStateEvent) {
            ctx.close();
            if (log.isDebugEnabled()) {
                log.debug("Idle channel={} closed", ctx.channel());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (log.isWarnEnabled()) {
            log.warn("Close error channel:{}", ctx.channel(), cause);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        Packet packet = null;

        Attribute<Session> attr = ctx.channel().attr(Session.SESSION_ATTRIBUTE_KEY);
        Session session = attr.get();

        int prefer = Protocol.RESERVED_ID;
        Protocol protocol = null;
        if (session != null) {
            protocol = session.preferredProtocol();
            prefer = protocol.id();
            packet = protocol.pack(in);
        }
        if (packet == null) {
            for (Protocol p : protocols) {
                if (p.id() == prefer) {
                    continue;
                }
                packet = p.pack(in);
                if (packet != null) {
                    protocol = p;
                    break;
                }
            }
        }
        if (protocol == null || packet == null) {
            throw new BadDataFormatException();
        }
        if (session == null) {
            session = new ShinySession(ctx.channel(), protocol);
            attr.setIfAbsent(session);
        }
        if (session.preferredProtocol() != protocol) {
            session.setPreferredProtocol(protocol);
        }
        handler.handle(session, packet);
    }
}
