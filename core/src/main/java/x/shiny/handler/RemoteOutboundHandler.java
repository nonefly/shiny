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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import x.shiny.Packet;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.channel.Session;
import x.shiny.channel.ShinySession;
import x.shiny.tcp.TCPClient;

/**
 * @author guohaoice@gmail.com
 */
@AllArgsConstructor
public class RemoteOutboundHandler {
    private final TCPClient client;

    public Future<Response> invoke(Request request) {
        Channel channel = client.getChannel();
        Session session;
        if (channel.hasAttr(Session.SESSION_ATTRIBUTE_KEY)) {
            session = channel.attr(Session.SESSION_ATTRIBUTE_KEY).get();
        } else {
            channel.attr(Session.SESSION_ATTRIBUTE_KEY)
                    .setIfAbsent(new ShinySession(channel, client.getProtocol()));
            session= channel.attr(Session.SESSION_ATTRIBUTE_KEY).get();
        }
        Promise<Response> promise = channel.eventLoop().newPromise();
        Packet packet = new RequestPacket(session.id(promise), request, session.preferredProtocol());
        ByteBuf message = session.preferredProtocol().unpack(packet);
        channel.writeAndFlush(message);
        return promise;
    }

    @AllArgsConstructor
    private final class RequestPacket implements Packet {
        private final int id;
        private final Request request;
        private final Protocol protocol;


        @Override
        public int id() {
            return id;
        }

        @Override
        public String method() {
            return request.method();
        }

        @Override
        public String service() {
            return request.service();
        }

        @Override
        public boolean isRequest() {
            return true;
        }

        @Override
        public Protocol protocol() {
            return protocol;
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public Response response() {
            return null;
        }
    }
}
