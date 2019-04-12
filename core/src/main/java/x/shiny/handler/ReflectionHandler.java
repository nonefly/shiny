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
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import x.shiny.Request;
import x.shiny.Response;
import x.shiny.invocation.InvocationHandler;
import x.shiny.invocation.Pipeline;

/**
 * @author guohaoice@gmail.com
 */
@AllArgsConstructor
public class ReflectionHandler implements InvocationHandler {
    private final List<Service> services;

    @Override
    public Future<Response> invoke(Pipeline context, Request request) {
        for (Service service : services) {
            Descriptors.ServiceDescriptor descriptor = service.getDescriptorForType();
            if (!descriptor.getFullName().equals(request.service())) {
                continue;
            }
            Descriptors.MethodDescriptor methodDescriptor = descriptor.findMethodByName(request.method());
            Message message = request.arg();
            Promise<Response> responsePromise = GlobalEventExecutor.INSTANCE.newPromise();
            RpcCallback<Message> callback = o -> {
                Response response = new Response() {
                    @Override
                    public Message bizResponse() {
                        return o;
                    }

                    @Override
                    public Throwable cause() {
                        return null;
                    }

                    @Override
                    public boolean success() {
                        return true;
                    }
                };
                responsePromise.setSuccess(response);

            };
            service.callMethod(methodDescriptor, null, message, callback);
            return responsePromise;
        }
        throw new IllegalStateException("No service found for request:" + request);
    }
}
