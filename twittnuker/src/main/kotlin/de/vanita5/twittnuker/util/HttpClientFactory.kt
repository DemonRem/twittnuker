/*
* Twittnuker - Twitter client for Android
*
* Copyright (C) 2013-2017 vanita5 <mail@vanit.as>
*
* This program incorporates a modified version of Twidere.
* Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.vanita5.twittnuker.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import okhttp3.*
import org.mariotaku.ktextension.toIntOr
import org.mariotaku.restfu.http.RestHttpClient
import org.mariotaku.restfu.okhttp3.OkHttpRestClient
import de.vanita5.twittnuker.constant.SharedPreferenceConstants.*
import de.vanita5.twittnuker.util.dagger.DependencyHolder
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object HttpClientFactory {

    fun createRestHttpClient(conf: HttpClientConfiguration, dns: Dns, connectionPool: ConnectionPool,
            cache: Cache): RestHttpClient {
        val builder = OkHttpClient.Builder()
        initOkHttpClient(conf, builder, dns, connectionPool, cache)
        return OkHttpRestClient(builder.build())
    }

    fun initOkHttpClient(conf: HttpClientConfiguration, builder: OkHttpClient.Builder, dns: Dns,
            connectionPool: ConnectionPool, cache: Cache) {
        updateHttpClientConfiguration(builder, conf, dns, connectionPool, cache)
        DebugModeUtils.initForOkHttpClient(builder)
    }

    internal fun updateHttpClientConfiguration(builder: OkHttpClient.Builder, conf: HttpClientConfiguration,
            dns: Dns, connectionPool: ConnectionPool, cache: Cache) {
        conf.applyTo(builder)
        builder.dns(dns)
        builder.connectionPool(connectionPool)
        builder.cache(cache)
    }

    class HttpClientConfiguration(val prefs: SharedPreferencesWrapper) {

        var readTimeoutSecs: Long = -1
        var writeTimeoutSecs: Long = -1
        var connectionTimeoutSecs: Long = prefs.getInt(KEY_CONNECTION_TIMEOUT, 10).toLong()
        var cacheSize: Int = prefs.getInt(KEY_CACHE_SIZE_LIMIT, 300).coerceIn(100..500)

        internal fun applyTo(builder: OkHttpClient.Builder) {
            if (connectionTimeoutSecs >= 0) {
                builder.connectTimeout(connectionTimeoutSecs, TimeUnit.SECONDS)
            }
            if (writeTimeoutSecs >= 0) {
                builder.writeTimeout(writeTimeoutSecs, TimeUnit.SECONDS)
            }
            if (readTimeoutSecs >= 0) {
                builder.readTimeout(readTimeoutSecs, TimeUnit.SECONDS)
            }
            if (prefs.getBoolean(KEY_ENABLE_PROXY, false)) {
                configProxy(builder)
            }
        }

        private fun configProxy(builder: OkHttpClient.Builder) {
            val proxyType = prefs.getString(KEY_PROXY_TYPE, null) ?: return
            val proxyHost = prefs.getString(KEY_PROXY_HOST, null)?.takeIf(String::isNotEmpty) ?: return
            val proxyPort = prefs.getString(KEY_PROXY_PORT, null).toIntOr(-1)
            val username = prefs.getString(KEY_PROXY_USERNAME, null)?.takeIf(String::isNotEmpty)
            val password = prefs.getString(KEY_PROXY_PASSWORD, null)?.takeIf(String::isNotEmpty)
            when (proxyType) {
                "http" -> {
                    if (proxyPort !in (0..65535)) {
                        return
                    }
                    val address = InetSocketAddress.createUnresolved(proxyHost, proxyPort)
                    builder.proxy(Proxy(Proxy.Type.HTTP, address))

                    builder.authenticator { _, response ->
                        val b = response.request().newBuilder()
                        if (response.code() == 407) {
                            if (username != null && password != null) {
                                val credential = Credentials.basic(username, password)
                                b.header("Proxy-Authorization", credential)
                            }
                        }
                        b.build()
                    }
                }
                "reverse" -> {
                    builder.addInterceptor(ReverseProxyInterceptor(proxyHost, username, password))
                }
            }

        }

    }

    fun reloadConnectivitySettings(context: Context) {
        val holder = DependencyHolder.get(context)
        val client = holder.restHttpClient as? OkHttpRestClient ?: return
        val builder = OkHttpClient.Builder()
        initOkHttpClient(HttpClientConfiguration(holder.preferences), builder, holder.dns,
                holder.connectionPool, holder.cache)
        client.client = builder.build()
    }

    /**
     * Intercept and replace proxy patterns to real URL
     */
    class ReverseProxyInterceptor(val proxyFormat: String, val proxyUsername: String?,
            val proxyPassword: String?) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url()
            val builder = request.newBuilder()
            val replacedUrl = HttpUrl.parse(replaceUrl(url, proxyFormat)) ?: run {
                throw IOException("Invalid reverse proxy format")
            }
            builder.url(replacedUrl)
            if (proxyUsername != null && proxyPassword != null) {
                val headerValue = Base64.encodeToString("$proxyUsername:$proxyPassword".toByteArray(Charsets.UTF_8),
                        Base64.URL_SAFE)
                builder.addHeader("Proxy-Authorization", headerValue)
            }
            return chain.proceed(builder.build())
        }

    }

    /**
     * # Supported patterns
     *
     * * `[SCHEME]`: E.g. `http` or `https`
     * * `[HOST]`: Host address
     * * `[PORT]`: Port number
     * * `[AUTHORITY]`: `[HOST]`:`[PORT]` or `[HOST]` if port is default. Colon **will be** URL encoded
     * * `[PATH]`: Raw path part, **without leading slash**
     * * `[/PATH]`: Raw path part, **with leading slash**
     * * `[PATH_ENCODED]`: Path, **will be** URL encoded again
     * * `[QUERY]`: Raw query part
     * * `[?QUERY]`: Raw query part, with `?` prefix
     * * `[QUERY_ENCODED]`: Raw query part, **will be** URL encoded again
     * * `[FRAGMENT]`: Raw fragment part
     * * `[#FRAGMENT]`: Raw fragment part, with `#` prefix
     * * `[FRAGMENT_ENCODED]`: Raw fragment part, **will be** URL encoded again
     * * `[URL_ENCODED]`: URL Encoded `url` itself
     * * `[URL_BASE64]`: Base64 Encoded `url` itself
     *
     * # Null values
     * `[PATH]`, `[/PATH]`, `[QUERY]`, `[?QUERY]`, `[FRAGMENT]`, `[#FRAGMENT]` will be empty when
     * it's null, values and base64-encoded will be string `"null"`.
     *
     * A valid format might looks like
     *
     * `https://proxy.com/[SCHEME]/[AUTHORITY]/[PATH][?QUERY][#FRAGMENT]`,
     *
     * A request
     *
     * `https://example.com:8080/path?query=value#fragment`
     *
     * Will be transformed to
     *
     * `https://proxy.com/https/example.com%3A8080/path?query=value#fragment`
     */
    @Suppress("KDocUnresolvedReference")
    fun replaceUrl(url: HttpUrl, format: String): String {
        val sb = StringBuffer()
        var startIndex = 0
        while (startIndex != -1) {
            val find = format.findAnyOf(urlSupportedPatterns, startIndex) ?: break
            sb.append(format, startIndex, find.first)
            sb.append(when (find.second) {
                "[SCHEME]" -> url.scheme()
                "[HOST]" -> url.host()
                "[PORT]" -> url.port()
                "[AUTHORITY]" -> url.authority()
                "[PATH]" -> url.encodedPath()?.removePrefix("/").orEmpty()
                "[/PATH]" -> url.encodedPath().orEmpty()
                "[PATH_ENCODED]" -> url.encodedPath()?.removePrefix("/")?.urlEncoded()
                "[QUERY]" -> url.encodedQuery().orEmpty()
                "[?QUERY]" -> url.encodedQuery()?.prefix("?").orEmpty()
                "[QUERY_ENCODED]" -> url.encodedQuery()?.urlEncoded()
                "[FRAGMENT]" -> url.encodedFragment().orEmpty()
                "[#FRAGMENT]" -> url.encodedFragment()?.prefix("#").orEmpty()
                "[FRAGMENT_ENCODED]" -> url.encodedFragment()?.urlEncoded()
                "[URL_ENCODED]" -> url.toString().urlEncoded()
                "[URL_BASE64]" -> Base64.encodeToString(url.toString().toByteArray(Charsets.UTF_8),
                        Base64.URL_SAFE)
                else -> throw AssertionError()
            })
            startIndex = find.first + find.second.length
        }
        sb.append(format, startIndex, format.length)
        return sb.toString()
    }

    private fun HttpUrl.authority(): String {
        val host = host()
        val port = port()
        if (port == HttpUrl.defaultPort(scheme())) return host
        return "$host%3A$port"
    }

    private fun String.urlEncoded() = Uri.encode(this)

    private fun String.prefix(prefix: String) = prefix + this

    private val urlSupportedPatterns = listOf("[SCHEME]", "[HOST]", "[PORT]", "[AUTHORITY]",
            "[PATH]", "[/PATH]", "[PATH_ENCODED]", "[QUERY]", "[?QUERY]", "[QUERY_ENCODED]",
            "[FRAGMENT]", "[#FRAGMENT]", "[FRAGMENT_ENCODED]", "[URL_ENCODED]", "[URL_BASE64]")
}