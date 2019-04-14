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

package x.shiny.protocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import x.shiny.Packet;
import x.shiny.Protocol;
import x.shiny.Request;
import x.shiny.Response;

/**
 * |0123 4567 | 0     123 4567     | 0123        4567 | 0123 4567        |
 * |BABE {id} | isReq    | bodyLen | serviceLen |serviceName| methodLen  |
 * |          methodName           | request/response body               |
 * ...
 *
 * @author guohaoice@gmail.com
 */
@Slf4j
public class ShinyProtocol implements Protocol {
    private static final byte[] MAGIC_HEADER = "BABE".getBytes(CharsetUtil.UTF_8);
    private static final int HEADER_LEN = 16;

    private ConcurrentMap<String, ProtoTypePair> typePairs = new ConcurrentHashMap<>();

    private Map<String, ProtoTypePair> serverProtoTypes = new HashMap<>();

    public ShinyProtocol(List<Service> serviceList) {
        if (serviceList != null) {
            for (Service service : serviceList) {
                String currService = service.getDescriptorForType().getFullName();
                List<Descriptors.MethodDescriptor> methods = service.getDescriptorForType().getMethods();
                for (Descriptors.MethodDescriptor method : methods) {
                    Message requestPrototype = service.getRequestPrototype(method);
                    Message responsePrototype = service.getResponsePrototype(method);
                    ProtoTypePair typePair = new ProtoTypePair(requestPrototype, responsePrototype);
                    serverProtoTypes.put(currService + "#" + method.getName(), typePair);
                }
            }
        }
    }

    @Override
    public int id() {
        return 0;
    }

    @Override
    public Packet pack(ByteBuf buf) {
        buf.markReaderIndex();
        if (buf.readableBytes() < HEADER_LEN) {
            return null;
        }
        buf.skipBytes(4);
        int id = buf.readInt();
        boolean isRequest = buf.readInt() == 1;
        int len = buf.readInt();
        if (buf.readableBytes() < len) {
            buf.resetReaderIndex();
            return null;
        }

        int serviceNameLen = buf.readInt();
        byte[] service = new byte[serviceNameLen];
        buf.readBytes(service);
        String serviceName = new String(service, CharsetUtil.UTF_8);

        int methodLen = buf.readInt();
        byte[] method = new byte[methodLen];
        buf.readBytes(method);
        String methodName = new String(method, CharsetUtil.UTF_8);

        byte[] body = new byte[len - serviceNameLen - methodLen];
        buf.readBytes(body);
        Message message;
        Message responseType;
        try {
            if (!isRequest) {
                ProtoTypePair pair = typePairs.get(serviceName + "#" + methodName);
                message = pair.response.getParserForType().parseFrom(body);
                responseType = pair.response;
            } else {
                message = getProtoType(serviceName, methodName, isRequest)
                        .getParserForType()
                        .parseFrom(body);
                responseType = getProtoType(serviceName, methodName, false);

            }
        } catch (InvalidProtocolBufferException e) {
            log.warn("Bad request format", e);
            return null;
        }

        Packet packet = new Packet() {
            @Override
            public int id() {
                return id;
            }

            @Override
            public String method() {
                return methodName;
            }

            @Override
            public String service() {
                return serviceName;
            }

            @Override
            public boolean isRequest() {
                return isRequest;
            }

            @Override
            public Protocol protocol() {
                return ShinyProtocol.this;
            }

            @Override
            public Request request() {
                if (!isRequest) {
                    return null;
                } else {
                    return new Request() {
                        @Override
                        public String service() {
                            return serviceName;
                        }

                        @Override
                        public String method() {
                            return methodName;
                        }

                        @Override
                        public Message arg() {
                            return message;
                        }

                        @Override
                        public Message responseType() {
                            return responseType;
                        }
                    };
                }
            }

            @Override
            public Response response() {
                if (isRequest) {
                    return null;
                } else {
                    Response response = new Response() {
                        @Override
                        public Message bizResponse() {
                            return message;
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
                    return response;
                }
            }
        };
        return packet;
    }

    @Override
    public ByteBuf unpack(Packet packet) {
        if (packet.isRequest()) {
            Request request = packet.request();
            String key = request.service() + "#" + request.method();
            if (!typePairs.containsKey(key)) {
                typePairs.putIfAbsent(key, new ProtoTypePair(request.arg().getDefaultInstanceForType(), request.responseType()));
            }
        }
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(MAGIC_HEADER);
        buf.writeInt(packet.id());
        buf.writeInt(packet.isRequest() ? 1 : 0);

        byte[] method = packet.method().getBytes(CharsetUtil.UTF_8);
        byte[] service = packet.service().getBytes(CharsetUtil.UTF_8);
        byte[] body = packet.isRequest() ? packet.request().arg().toByteArray() : packet.response().bizResponse().toByteArray();
        buf.writeInt(body.length + method.length + service.length);
        buf.writeInt(service.length);
        buf.writeBytes(service);
        buf.writeInt(method.length);
        buf.writeBytes(method);
        buf.writeBytes(body);
        return buf;
    }


    private Message getProtoType(String serviceName, String methodName, boolean isRequest) throws InvalidProtocolBufferException {
        ProtoTypePair typePair = serverProtoTypes.get(serviceName + "#" + methodName);
        if (typePair == null) {
            throw new InvalidProtocolBufferException("404");
        }
        return isRequest ? typePair.getRequest() : typePair.getResponse();
    }

    @Data
    @AllArgsConstructor
    private static final class ProtoTypePair {
        private final Message request;
        private final Message response;
    }
}
