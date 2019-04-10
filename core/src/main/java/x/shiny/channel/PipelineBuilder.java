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

import java.util.List;
import com.google.protobuf.Service;
import x.shiny.filter.ReflectionHandler;
import x.shiny.filter.inbound.RemoteHandler;
import x.shiny.filter.outbound.OutboundRemoteHandler;

/**
 * @author guohaoice@gmail.com
 */
public class PipelineBuilder {
    public static InvocationPipeline buildRequestPipeline(List<Service> services) {
        ReflectionHandler handler = new ReflectionHandler(services);
        InvocationPipeline current = new ShinyInvocationPipeline(null, handler);
        current = new ShinyInvocationPipeline(current, new OutboundRemoteHandler());
    }

    public static InvocationPipeline buildResponsePipeline() {
        InvocationPipeline tail = new ShinyInvocationPipeline(null, new RemoteHandler());

        return tail;
    }
}
