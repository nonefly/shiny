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

package x.shiny.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author guohaoice@gmail.com
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public class RPCException extends RuntimeException {
    public static final RPCException BAD_DATA_FORMAT = new RPCException(403, "Bad data format");
    public static final RPCException NOT_FOUND_METHOD = new RPCException(404, "Can not found method");
    private final int code;
    private final String msg;
}
