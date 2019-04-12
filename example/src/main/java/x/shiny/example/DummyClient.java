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

package x.shiny.example;

import lombok.extern.slf4j.Slf4j;
import x.shiny.Endpoint;
import x.shiny.common.DefaultEndpoint;
import x.shiny.example.proto.EchoRequest;
import x.shiny.example.proto.EchoService;
import x.shiny.tcp.TCPClient;

/**
 * @author guohaoice@gmail.com
 */
@Slf4j
public class DummyClient {
    public static void main(String[] args) {
        Endpoint endpoint = new DefaultEndpoint("localhost", 8888);

        TCPClient client = new TCPClient(endpoint);
        EchoService.Stub stub = EchoService.newStub(client);

        client.connect();

        stub.echo(null, EchoRequest.newBuilder().setName("haha").build(), r -> {
            if (log.isInfoEnabled()) {
                log.info("Response:{}", r.getName());
            }
        });
    }
}
