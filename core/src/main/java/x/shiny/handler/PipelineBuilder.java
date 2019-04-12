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
import com.google.protobuf.Service;
import io.netty.util.concurrent.Future;
import lombok.AllArgsConstructor;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.invocation.InvocationHandler;
import x.shiny.invocation.Pipeline;

/**
 * @author guohaoice@gmail.com
 */
public class PipelineBuilder {
    public static Pipeline buildRequestPipeline(List<Service> services) {
        ReflectionHandler handler = new ReflectionHandler(services);
        PipelineNode node = new PipelineNode(handler, null);
        return node;
    }

    @AllArgsConstructor
    private static final class PipelineNode implements Pipeline {
        private final InvocationHandler handler;
        private final PipelineNode next;


        @Override
        public Future<Response> invoke(Request request) {
            return handler.invoke(next, request);
        }
    }
}
