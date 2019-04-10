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

package x.shiny.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import x.shiny.Packet;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;

/**
 * @author guohaoice@gmail.com
 */
@AllArgsConstructor
public class HeadInvocationContext implements InvocationContext {
    private final Session session;
    private final HeadInvocationContext next;

    @Override
    public Protocol protocol() {
        return session.preferredProtocol();
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public Future<Response> invoke(Request request) {
        return null;
    }
    public Future<Response> invoke(Packet packet){
        Channel channel = session().channel();
        Future<Response> invoke =next.invoke(request)
                .addListener(future -> {
                    channel.write(future.get());
                });
        return invoke;
    }
}
