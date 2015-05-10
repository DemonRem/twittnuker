/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.simplerestapi.http.RestRequest;
import org.mariotaku.simplerestapi.http.RestResponse;
import de.vanita5.twittnuker.api.twitter.model.impl.RateLimitStatusJSONImpl;

import java.util.Locale;

import twitter4j.http.HttpResponseCode;
import twitter4j.internal.util.InternalParseUtil;

/**
 * An exception class that will be thrown when TwitterAPI calls are failed.<br>
 * In case the Twitter server returned HTTP error code, you can get the HTTP
 * status code using getStatusCode() method.
 * 
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
@JsonObject
public class TwitterException extends Exception implements TwitterResponse, HttpResponseCode {

    @JsonField(name = "errors")
    ErrorInfo[] errors;

	private int statusCode = -1;
    private RateLimitStatus rateLimitStatus;

    public ErrorInfo[] getErrors() {
        return errors;
    }

	private static final long serialVersionUID = -2623309261327598087L;


	boolean nested = false;
    private RestRequest request;
    private RestResponse response;

    public TwitterException() {
    }

	public TwitterException(final Exception cause) {
		this(cause.getMessage(), cause);
		if (cause instanceof TwitterException) {
			((TwitterException) cause).setNested();
		}
	}

	public TwitterException(final String message) {
		this(message, (Throwable) null);
	}

	public TwitterException(final String message, final Exception cause, final int statusCode) {
		this(message, cause);
		this.statusCode = statusCode;
	}

    public TwitterException(final String message, final RestRequest req, final RestResponse res) {
		this(message);
        setResponse(res);
		request = req;
        statusCode = res != null ? res.getStatus() : -1;
    }

    private void setResponse(RestResponse res) {
        response = res;
        if (res != null) {
            rateLimitStatus = RateLimitStatusJSONImpl.createFromResponseHeader(res);
		}
	}

    public TwitterException(final String message, final RestResponse res) {
		this(message, null, res);
	}

	public TwitterException(final String message, final Throwable cause) {
		super(message, cause);
	}


	/**
	 * Tests if the exception is caused by rate limitation exceed
	 * 
	 * @return if the exception is caused by rate limitation exceed
	 * @see <a href="https://dev.twitter.com/docs/rate-limiting">Rate Limiting |
	 *      Twitter Developers</a>
	 * @since Twitter4J 2.1.2
	 */
	public boolean exceededRateLimitation() {
		return statusCode == 400 && getRateLimitStatus() != null // REST API
				|| statusCode == ENHANCE_YOUR_CLAIM // Streaming API
				|| statusCode == TOO_MANY_REQUESTS; // API 1.1
	}

    @Override
    public void processResponseHeader(RestResponse resp) {

    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getAccessLevel() {
		return InternalParseUtil.toAccessLevel(response);
	}

	public int getErrorCode() {
        if (errors == null || errors.length == 0) return -1;
        return errors[0].getCode();
	}

	/**
	 * Returns error message from the API if available.
	 * 
	 * @return error message from the API
	 * @since Twitter4J 2.2.3
	 */
	public String getErrorMessage() {
        if (errors == null || errors.length == 0) return null;
        return errors[0].getMessage();
	}


    public RestRequest getHttpRequest() {
		return request;
	}

    public RestResponse getHttpResponse() {
		return response;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMessage() {
        if (errors != null && errors.length > 0)
            return String.format(Locale.US, "Error %d: %s", errors[0].getCode(), errors[0].getMessage());
		else if (statusCode != -1)
			return String.format(Locale.US, "Error %d", statusCode);
		else
			return super.getMessage();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since Twitter4J 2.1.2
	 */
	@Override
	public RateLimitStatus getRateLimitStatus() {
        return rateLimitStatus;
	}

	public String getResponseHeader(final String name) {
		if (response != null) {
            return response.getHeader(name);
		}
        return null;
	}

	/**
	 * Returns int value of "Retry-After" response header (Search API) or
	 * seconds_until_reset (REST API). An application that exceeds the rate
	 * limitations of the Search API will receive HTTP 420 response codes to
	 * requests. It is a best practice to watch for this error condition and
	 * honor the Retry-After header that instructs the application when it is
	 * safe to continue. The Retry-After header's value is the number of seconds
	 * your application should wait before submitting another query (for
	 * example: Retry-After: 67).<br>
	 * Check if getStatusCode() == 503 before calling this method to ensure that
	 * you are actually exceeding rate limitation with query apis.<br>
	 * 
	 * @return instructs the application when it is safe to continue in seconds
	 * @see <a href="https://dev.twitter.com/docs/rate-limiting">Rate Limiting |
	 *      Twitter Developers</a>
	 * @since Twitter4J 2.1.0
	 */
	public int getRetryAfter() {
		int retryAfter = -1;
		if (statusCode == 400) {
			final RateLimitStatus rateLimitStatus = getRateLimitStatus();
			if (rateLimitStatus != null) {
				retryAfter = rateLimitStatus.getSecondsUntilReset();
			}
		} else if (statusCode == ENHANCE_YOUR_CLAIM) {
			try {
                final String retryAfterStr = response.getHeader("Retry-After");
				if (retryAfterStr != null) {
					retryAfter = Integer.valueOf(retryAfterStr);
				}
			} catch (final NumberFormatException ignore) {
			}
		}
		return retryAfter;
	}

	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Tests if the exception is caused by network issue
	 * 
	 * @return if the exception is caused by network issue
	 * @since Twitter4J 2.1.2
	 */
	public boolean isCausedByNetworkIssue() {
		return getCause() instanceof java.io.IOException;
	}

	/**
	 * Tests if error message from the API is available
	 * 
	 * @return true if error message from the API is available
	 * @since Twitter4J 2.2.3
	 */
	public boolean isErrorMessageAvailable() {
        return errors != null && errors.length > 0;
	}

	/**
	 * Tests if the exception is caused by non-existing resource
	 * 
	 * @return if the exception is caused by non-existing resource
	 * @since Twitter4J 2.1.2
	 */
	public boolean resourceNotFound() {
		return statusCode == NOT_FOUND;
	}

	@Override
	public String toString() {
		return getMessage();
	}


	void setNested() {
		nested = true;
	}
}