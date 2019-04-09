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

package x.shiny.util;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author guohaoice@gmail.com
 */
public final class NetElf {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public static int randomPort() {
        return randomPort(MIN_PORT, MAX_PORT);
    }

    public static int randomPort(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return port;
        } catch (IOException e) {
            throw new IllegalStateException("Bind failed to:" + port);
        }
    }

    public static int randomPort(int start, int end) {
        int least = Math.max(MIN_PORT, start);
        int bound = Math.min(MAX_PORT, end);
        int maxAttempt = 10;
        for (int i = 0; i < maxAttempt; i++) {
            int port = RandomElf.random(least, bound);
            try {
                return randomPort(port);
            } catch (IllegalStateException e) {
                // ignored
            }
        }
        throw new IllegalStateException("Can not find any available port from:" + start + " to:" + end);
    }
}
