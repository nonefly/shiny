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

import io.netty.util.concurrent.Future;
import lombok.AllArgsConstructor;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.filter.Filter;

/**
 * @author guohaoice@gmail.com
 */
@AllArgsConstructor
public class ShinyInvocationPipeline implements InvocationPipeline {
    private final InvocationPipeline next;
    private final Filter current;

    @Override
    public Future<Response> invoke(InvocationContext ctx, Request request) {
        return current.invoke(ctx, request);
    }

    @Override
    public InvocationPipeline next() {
        return next;
    }

}
