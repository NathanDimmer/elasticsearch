/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent;

import com.fasterxml.jackson.dataformat.cbor.CBORConstants;
import com.fasterxml.jackson.dataformat.smile.SmileConstants;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.cbor.CborXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.smile.SmileXContent;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * A one stop to use {@link org.elasticsearch.common.xcontent.XContent} and {@link XContentBuilder}.
 */
public class XContentFactory {

    private static int GUESS_HEADER_LENGTH = 20;

    /**
     * Returns a content builder using JSON format ({@link org.elasticsearch.common.xcontent.XContentType#JSON}.
     */
    public static XContentBuilder jsonBuilder() throws IOException {
        return contentBuilder(XContentType.JSON);
    }

    /**
     * Constructs a new json builder that will output the result into the provided output stream.
     */
    public static XContentBuilder jsonBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(JsonXContent.jsonXContent, os);
    }

    /**
     * Returns a content builder using SMILE format ({@link org.elasticsearch.common.xcontent.XContentType#SMILE}.
     */
    public static XContentBuilder smileBuilder() throws IOException {
        return contentBuilder(XContentType.SMILE);
    }

    /**
     * Constructs a new json builder that will output the result into the provided output stream.
     */
    public static XContentBuilder smileBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(SmileXContent.smileXContent, os);
    }

    /**
     * Returns a content builder using YAML format ({@link org.elasticsearch.common.xcontent.XContentType#YAML}.
     */
    public static XContentBuilder yamlBuilder() throws IOException {
        return contentBuilder(XContentType.YAML);
    }

    /**
     * Constructs a new yaml builder that will output the result into the provided output stream.
     */
    public static XContentBuilder yamlBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(YamlXContent.yamlXContent, os);
    }

    /**
     * Returns a content builder using CBOR format ({@link org.elasticsearch.common.xcontent.XContentType#CBOR}.
     */
    public static XContentBuilder cborBuilder() throws IOException {
        return contentBuilder(XContentType.CBOR);
    }

    /**
     * Constructs a new cbor builder that will output the result into the provided output stream.
     */
    public static XContentBuilder cborBuilder(OutputStream os) throws IOException {
        return new XContentBuilder(CborXContent.cborXContent, os);
    }

    /**
     * Constructs a xcontent builder that will output the result into the provided output stream.
     */
    public static XContentBuilder contentBuilder(XContentType type, OutputStream outputStream) throws IOException {
        if (type == XContentType.JSON) {
            return jsonBuilder(outputStream);
        } else if (type == XContentType.SMILE) {
            return smileBuilder(outputStream);
        } else if (type == XContentType.YAML) {
            return yamlBuilder(outputStream);
        } else if (type == XContentType.CBOR) {
            return cborBuilder(outputStream);
        }
        throw new ElasticsearchIllegalArgumentException("No matching content type for " + type);
    }

    /**
     * Returns a binary content builder for the provided content type.
     */
    public static XContentBuilder contentBuilder(XContentType type) throws IOException {
        if (type == XContentType.JSON) {
            return JsonXContent.contentBuilder();
        } else if (type == XContentType.SMILE) {
            return SmileXContent.contentBuilder();
        } else if (type == XContentType.YAML) {
            return YamlXContent.contentBuilder();
        } else if (type == XContentType.CBOR) {
            return CborXContent.contentBuilder();
        }
        throw new ElasticsearchIllegalArgumentException("No matching content type for " + type);
    }

    /**
     * Returns the {@link org.elasticsearch.common.xcontent.XContent} for the provided content type.
     */
    public static XContent xContent(XContentType type) {
        return type.xContent();
    }

    /**
     * Guesses the content type based on the provided char sequence.
     */
    public static XContentType xContentType(CharSequence content) {
        int length = content.length() < GUESS_HEADER_LENGTH ? content.length() : GUESS_HEADER_LENGTH;
        if (length == 0) {
            return null;
        }
        char first = content.charAt(0);
        if (first == '{') {
            return XContentType.JSON;
        }
        // Should we throw a failure here? Smile idea is to use it in bytes....
        if (length > 2 && first == SmileConstants.HEADER_BYTE_1 && content.charAt(1) == SmileConstants.HEADER_BYTE_2 && content.charAt(2) == SmileConstants.HEADER_BYTE_3) {
            return XContentType.SMILE;
        }
        if (length > 2 && first == '-' && content.charAt(1) == '-' && content.charAt(2) == '-') {
            return XContentType.YAML;
        }

        // CBOR is not supported because it is a binary-only format

        for (int i = 1; i < length; i++) {
            char c = content.charAt(i);
            if (c == '{') {
                return XContentType.JSON;
            }
        }
        return null;
    }

    /**
     * Guesses the content (type) based on the provided char sequence.
     */
    public static XContent xContent(CharSequence content) {
        XContentType type = xContentType(content);
        if (type == null) {
            throw new ElasticsearchParseException("Failed to derive xcontent from " + content);
        }
        return xContent(type);
    }

    /**
     * Guesses the content type based on the provided bytes.
     */
    public static XContent xContent(byte[] data) {
        return xContent(data, 0, data.length);
    }

    /**
     * Guesses the content type based on the provided bytes.
     */
    public static XContent xContent(byte[] data, int offset, int length) {
        XContentType type = xContentType(data, offset, length);
        if (type == null) {
            throw new ElasticsearchParseException("Failed to derive xcontent from (offset=" + offset + ", length=" + length + "): " + Arrays.toString(data));
        }
        return xContent(type);
    }

    /**
     * Guesses the content type based on the provided bytes.
     */
    public static XContentType xContentType(byte[] data) {
        return xContentType(data, 0, data.length);
    }

    /**
     * Guesses the content type based on the provided input stream.
     */
    public static XContentType xContentType(InputStream si) throws IOException {
        // Need minimum of 3 bytes for everything (except really JSON)
        int first = si.read();
        int second = si.read();
        int third = si.read();

        // Cannot short circuit based on third (or later fourth) because "{}" is valid JSON
        if (first == -1 || second == -1) {
            return null;
        }

        if (first == SmileConstants.HEADER_BYTE_1 && second == SmileConstants.HEADER_BYTE_2 &&
            third == SmileConstants.HEADER_BYTE_3) {
            return XContentType.SMILE;
        }
        if (first == '-' && second == '-' && third == '-') {
            return XContentType.YAML;
        }

        // Need 4 bytes for CBOR
        int fourth = si.read();

        if (first == '{' || second == '{' || third == '{' || fourth == '{') {
            return XContentType.JSON;
        }

        // ensure that we don't only have two or three bytes (definitely not CBOR and everything else was checked)
        if (third != -1 && fourth != -1) {
            if (isCBORObjectHeader((byte)first, (byte)second, (byte)third, (byte)fourth)) {
                return XContentType.CBOR;
            }

            for (int i = 4; i < GUESS_HEADER_LENGTH; i++) {
                int val = si.read();
                if (val == -1) {
                    return null;
                }
                if (val == '{') {
                    return XContentType.JSON;
                }
            }
        }

        return null;
    }

    /**
     * Guesses the content type based on the provided bytes.
     */
    public static XContentType xContentType(byte[] data, int offset, int length) {
        return xContentType(new BytesArray(data, offset, length));
    }

    public static XContent xContent(BytesReference bytes) {
        XContentType type = xContentType(bytes);
        if (type == null) {
            throw new ElasticsearchParseException("Failed to derive xcontent from " + bytes);
        }
        return xContent(type);
    }

    /**
     * Guesses the content type based on the provided bytes.
     */
    public static XContentType xContentType(BytesReference bytes) {
        int length = bytes.length() < GUESS_HEADER_LENGTH ? bytes.length() : GUESS_HEADER_LENGTH;
        if (length == 0) {
            return null;
        }
        byte first = bytes.get(0);
        if (first == '{') {
            return XContentType.JSON;
        }
        if (length > 2) {
            byte second = bytes.get(1);
            byte third = bytes.get(2);

            if (first == SmileConstants.HEADER_BYTE_1 && second == SmileConstants.HEADER_BYTE_2 && third == SmileConstants.HEADER_BYTE_3) {
                return XContentType.SMILE;
            }
            if (first == '-' && second == '-' && third == '-') {
                return XContentType.YAML;
            }
            if (length > 3 && isCBORObjectHeader(first, second, third, bytes.get(3))) {
                return XContentType.CBOR;
            }
            // note: technically this only needs length >= 2, but if the string is just " {", then it's not JSON, but
            //  " {}" is JSON (3 characters)
            for (int i = 1; i < length; i++) {
                if (bytes.get(i) == '{') {
                    return XContentType.JSON;
                }
            }
        }
        return null;
    }

    /**
     * Determine if the specified bytes represent a CBOR encoded stream.
     * <p />
     * This performs two checks to verify that it is indeed valid/usable CBOR data:
     * <ol>
     * <li>Checks the first three bytes for the
     * {@link CBORConstants#TAG_ID_SELF_DESCRIBE self-identifying CBOR tag (header)}</li>
     * <li>Checks that the fourth byte represents a major type that is an object, as opposed to some other type (e.g.,
     * text)</li>
     * </ol>
     *
     * @param first The first byte of the header
     * @param second The second byte of the header
     * @param third The third byte of the header
     * @param fourth The fourth byte represents the <em>first</em> byte of the CBOR data, indicating the data's type
     *
     * @return {@code true} if a CBOR byte stream is detected. {@code false} otherwise.
     */
    private static boolean isCBORObjectHeader(byte first, byte second, byte third, byte fourth) {
        // Determine if it uses the ID TAG (0xd9f7), then see if it starts with an object (equivalent to
        //  checking in JSON if it starts with '{')
        return CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_TAG, first) &&
               (((second << 8) & 0xff00) | (third & 0xff)) == CBORConstants.TAG_ID_SELF_DESCRIBE &&
               CBORConstants.hasMajorType(CBORConstants.MAJOR_TYPE_OBJECT, fourth);
    }
}
