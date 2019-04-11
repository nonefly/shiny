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

import java.util.List;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
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
    private List<Service> serviceList;

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
        String methodName = new String(service, CharsetUtil.UTF_8);

        byte[] body = new byte[len - serviceNameLen - methodLen];
        buf.readBytes(body);
        Message message;
        try {
            message = getProtoType(serviceName, methodName, isRequest).toProto().getParserForType().parseFrom(body);
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

    public void setServiceList(List<Service> serviceList) {
        this.serviceList = serviceList;
    }

    private Descriptors.Descriptor getProtoType(String serviceName, String methodName, boolean isRequest) throws InvalidProtocolBufferException {
        for (Service service : serviceList) {
            if (service.getDescriptorForType().getName().equals(serviceName)) {
                Descriptors.MethodDescriptor descriptor = service.getDescriptorForType().findMethodByName(methodName);
                return isRequest ? descriptor.getInputType() : descriptor.getOutputType();
            }
        }
        throw new InvalidProtocolBufferException(serviceName + " " + methodName);
    }
}
