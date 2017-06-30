/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.runtime;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Date;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Used in combination with the generated member copiers to implement deep
 * copies of shape members.
 */
@SdkInternalApi
public final class StandardMemberCopier {
    public static String copy(String s) {
        return s;
    }

    public static Short copy(Short s) {
        return s;
    }

    public static Integer copy(Integer i) {
        return i;
    }

    public static Long copy(Long l) {
        return l;
    }

    public static Float copy(Float f) {
        return f;
    }

    public static Double copy(Double d) {
        return d;
    }

    public static BigDecimal copy(BigDecimal bd) {
        return bd;
    }

    public static Boolean copy(Boolean b) {
        return b;
    }

    public static InputStream copy(InputStream is) {
        return is;
    }

    public static Date copy(Date d) {
        if (d == null) {
            return null;
        }
        return new Date(d.getTime());
    }

    public static ByteBuffer copy(ByteBuffer bb) {
        if (bb == null) {
            return null;
        }

        ByteBuffer copy = bb.isDirect() ? ByteBuffer.allocateDirect(bb.capacity()) : ByteBuffer.allocate(bb.capacity());

        // Copy content
        ByteBuffer toCopy = bb.asReadOnlyBuffer();
        toCopy.rewind();
        copy.put(toCopy);

        // Copy non-content
        copy.position(bb.position());
        copy.limit(bb.limit());
        copy.order(bb.order());

        return copy.asReadOnlyBuffer();
    }
}