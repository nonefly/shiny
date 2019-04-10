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

package x.shiny.filter.outbound;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.channel.InvocationContext;
import x.shiny.filter.Filter;

/**
 * @author guohaoice@gmail.com
 */
public class OutboundRemoteHandler implements Filter {
    @Override
    public Future<Response> invoke(InvocationContext context, Request request) {
        Channel channel = context.session().channel();
        Future<Response> invoke = context.invoke(request)
                .addListener(future -> {
                    channel.write(future.get());
                });
        return invoke;
    }
}
