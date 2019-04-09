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

package x.shiny.filter.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import x.shiny.Packet;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.channel.InvocationContext;
import x.shiny.filter.Filter;

/**
 * @author xiangsheng.gh
 */
@Slf4j
public class RemoteHandler implements Filter {

    @Override
    public Future<Response> invoke(InvocationContext context, Request request) {
        ByteBuf byteBuf = context.protocol().encode(new Packet() {
            @Override
            public boolean isRequest() {
                return true;
            }

            @Override
            public Request request() {
                return request;
            }

            @Override
            public Response response() {
                return null;
            }
        });

        final Channel channel = context.session().channel();
        ChannelFuture f = channel.write(byteBuf);
        Promise<Response> promise = GlobalEventExecutor.INSTANCE.newPromise();
        return promise;
    }
}
