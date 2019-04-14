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
import io.netty.util.concurrent.Future;
import lombok.AllArgsConstructor;
import x.shiny.Packet;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.channel.Session;
import x.shiny.invocation.Filter;

/**
 * @author guohaoice@gmail.com
 */
@AllArgsConstructor
public class RemoteInboundHandler {
    private final Filter filter;

    public void handle(Session session, Packet packet) {
        Channel channel = session.channel();
        if (packet.isRequest()) {
            Future<Response> f = filter.invoke(packet.request());
            f.addListener(future -> {
                Response response = (Response) future.get();
                ByteBuf message = packet.protocol()
                        .unpack(new ResponsePacket(response, packet));
                channel.writeAndFlush(message);
            });
        } else {
            session.promise(packet.id())
                    .setSuccess(packet.response());
        }
    }

    @AllArgsConstructor
    private final class ResponsePacket implements Packet {
        private final Response response;
        private final Packet requestPacket;

        @Override
        public int id() {
            return requestPacket.id();
        }

        @Override
        public String method() {
            return requestPacket.method();
        }

        @Override
        public String service() {
            return requestPacket.service();
        }

        @Override
        public boolean isRequest() {
            return false;
        }

        @Override
        public Protocol protocol() {
            return requestPacket.protocol();
        }

        @Override
        public Request request() {
            return requestPacket.request();
        }

        @Override
        public Response response() {
            return response;
        }
    }

}
