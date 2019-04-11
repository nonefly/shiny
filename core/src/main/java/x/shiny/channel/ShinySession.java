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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import x.shiny.Protocol;
import x.shiny.Response;

/**
 * @author guohaoice@gmail.com
 */
public class ShinySession implements Session {
    private final Channel channel;
    private final Map<Integer, Promise<Response>> ids = new HashMap<>();
    private volatile Protocol preferProtocol;
    private AtomicInteger igGen = new AtomicInteger(0);

    public ShinySession(Channel channel, Protocol preferProtocol) {
        this.channel = channel;
        this.preferProtocol = preferProtocol;
    }

    @Override
    public Protocol preferredProtocol() {
        return preferProtocol;
    }

    @Override
    public void setPreferredProtocol(Protocol protocol) {
        this.preferProtocol = protocol;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public int id(Promise<Response> promise) {
        int id = igGen.incrementAndGet();
        ids.put(id, promise);
        return id;
    }

    @Override
    public Promise<Response> promise(int id) {
        return ids.remove(id);
    }
}
