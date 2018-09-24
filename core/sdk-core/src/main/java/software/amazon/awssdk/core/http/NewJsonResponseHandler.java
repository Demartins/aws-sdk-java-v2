/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.protocol.json.JsonProtocolUnmarshaller;
import software.amazon.awssdk.core.protocol.SdkPojo;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Default implementation of HttpResponseHandler that handles a successful response from a
 * service and unmarshalls the result using a JSON unmarshaller.
 *
 * @param <T> Indicates the type being unmarshalled by this response handler.
 */
@SdkProtectedApi
@ReviewBeforeRelease("Metadata in base result has been broken. Fix this and deal with AwsResponseHandlerAdapter")
public final class NewJsonResponseHandler<T extends SdkPojo> implements HttpResponseHandler<T> {

    private final Supplier<SdkPojo> pojoSupplier;
    private final boolean needsConnectionLeftOpen;
    private final boolean isPayloadJson;
    /**
     * The JSON unmarshaller to use when handling the response
     */
    private JsonProtocolUnmarshaller<T> unmarshaller;

    /**
     * Constructs a new response handler that will use the specified JSON unmarshaller to unmarshall
     * the service response and uses the specified response element path to find the root of the
     * business data in the service's response.
     *
     * @param unmarshaller    The JSON unmarshaller to use on the response.
     */
    public NewJsonResponseHandler(JsonProtocolUnmarshaller<T> unmarshaller,
                                  Supplier<SdkPojo> pojoSupplier,
                                  boolean needsConnectionLeftOpen,
                                  boolean isPayloadJson) {
        this.unmarshaller = paramNotNull(unmarshaller, "unmarshaller");
        this.pojoSupplier = pojoSupplier;

        this.needsConnectionLeftOpen = needsConnectionLeftOpen;
        this.isPayloadJson = isPayloadJson;

    }


    /**
     * @see HttpResponseHandler#handle(SdkHttpFullResponse, ExecutionAttributes)
     */
    public T handle(SdkHttpFullResponse response, ExecutionAttributes executionAttributes) throws Exception {
        SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Parsing service response JSON.");

        SdkStandardLogger.REQUEST_ID_LOGGER.debug(() -> X_AMZN_REQUEST_ID_HEADER + " : " +
                                                        response.firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                                                .orElse("not available"));


        try {
            T result = unmarshaller.unmarshall(pojoSupplier, response);
            if(result == null) {
                // todo hack
                result = (T) ((SdkBuilder) pojoSupplier.get()).build();
            }

            // Make sure we read all the data to get an accurate CRC32 calculation.
            // See https://github.com/aws/aws-sdk-java/issues/1018
            if (shouldParsePayloadAsJson() && response.content().isPresent()) {
                IoUtils.drainInputStream(response.content().get());
            }

            SdkStandardLogger.REQUEST_LOGGER.trace(() -> "Done parsing service response.");
            return result;
        } finally {
            if (!needsConnectionLeftOpen) {
                response.content().ifPresent(i -> FunctionalUtils.invokeSafely(i::close));
            }
        }
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return needsConnectionLeftOpen;
    }

    /**
     * @return True if the payload will be parsed as JSON, false otherwise.
     */
    private boolean shouldParsePayloadAsJson() {
        return !needsConnectionLeftOpen && isPayloadJson;
    }

}
