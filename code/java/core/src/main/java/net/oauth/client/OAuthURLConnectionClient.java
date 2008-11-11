/*
 * Copyright 2008 Netflix, Inc.
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

package net.oauth.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.oauth.http.HttpMessage;
import net.oauth.http.HttpResponseMessage;

/**
 * Utility methods for an OAuth client based on HttpURLConnection.
 * <p>
 * httpclient3.OAuthHttpClient or httpclient4.OAuthHttpClient perform better
 * than this class, as a rule; since they do things like connection pooling.
 * 
 * @author John Kristian
 */
public class OAuthURLConnectionClient extends OAuthClient {

    /** Send a message to the service provider and get the response. */
    @Override
    protected HttpResponseMessage invoke(HttpMessage request) throws IOException {
        final String httpMethod = request.method;
        final Collection<Map.Entry<String, String>> addHeaders = request.headers;
        final URL url = request.url;
        final URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;
            http.setRequestMethod(httpMethod);
            http.setInstanceFollowRedirects(false);
        }
        StringBuilder headers = new StringBuilder(httpMethod);
        {
            headers.append(" ").append(url.getPath());
            String query = url.getQuery();
            if (query != null && query.length() > 0) {
                headers.append("?").append(query);
            }
            headers.append(EOL);
            for (Map.Entry<String, List<String>> header : connection
                    .getRequestProperties().entrySet()) {
                String key = header.getKey();
                for (String value : header.getValue()) {
                    headers.append(key).append(": ").append(value).append(EOL);
                }
            }
        }
        String contentLength = null;
        for (Map.Entry<String, String> header : addHeaders) {
            String key = header.getKey();
            if (CONTENT_LENGTH.equalsIgnoreCase(key) && connection instanceof HttpURLConnection) {
                contentLength = header.getValue();
            } else {
                connection.setRequestProperty(key, header.getValue());
            }
            headers.append(key).append(": ").append(header.getValue()).append(EOL);
        }
        byte[] excerpt = null;
        final InputStream body = request.getBody();
        if (body != null) {
            if (contentLength != null) {
                ((HttpURLConnection) connection)
                    .setFixedLengthStreamingMode(Integer.parseInt(contentLength));
            }
            connection.setDoOutput(true);
            OutputStream output = connection.getOutputStream();
            try {
                excerpt = ExcerptInputStream.copyAll(body, output);
            } finally {
                output.close();
            }
        }
        return new URLConnectionResponse(request, headers.toString(), excerpt, connection);
    }

    private static final String EOL = HttpResponseMessage.EOL;

}
