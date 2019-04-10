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

import java.util.Collections;
import java.util.List;
import x.shiny.tcp.TCPServer;

/**
 * @author guohaoice@gmail.com
 */
public class DummyServer {
    public static void main(String[] args) {
        List<Object> services = Collections.singletonList(new EchoServiceImpl());
        TCPServer server = new TCPServer(8888, services);
        server.start();
    }
}
