//
// Decompiled by Jadx - 988ms
//
package cn.hutool.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.BytesResource;
import cn.hutool.core.io.resource.FileResource;
import cn.hutool.core.io.resource.MultiFileResource;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.map.TableMap;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.-$;
import cn.hutool.http.HttpInterceptor;
import cn.hutool.http.body.FormUrlEncodedBody;
import cn.hutool.http.body.MultipartBody;
import cn.hutool.http.body.RequestBody;
import cn.hutool.http.body.ResourceBody;
import cn.hutool.http.cookie.GlobalCookieManager;
import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public class HttpRequest extends HttpBase<HttpRequest> {
    private HttpConfig config;
    private String cookie;
    private Map<String, Object> form;
    private HttpConnection httpConnection;
    private boolean isMultiPart;
    private boolean isRest;
    private Method method;
    private int redirectCount;
    private UrlBuilder url;
    private URLStreamHandler urlHandler;

    public static HttpRequest post(String str) {
        return of(str).method(Method.POST);
    }

    public static HttpRequest get(String str) {
        return of(str).method(Method.GET);
    }

    public static HttpRequest head(String str) {
        return of(str).method(Method.HEAD);
    }

    public static HttpRequest options(String str) {
        return of(str).method(Method.OPTIONS);
    }

    public static HttpRequest put(String str) {
        return of(str).method(Method.PUT);
    }

    public static HttpRequest patch(String str) {
        return of(str).method(Method.PATCH);
    }

    public static HttpRequest delete(String str) {
        return of(str).method(Method.DELETE);
    }

    public static HttpRequest trace(String str) {
        return of(str).method(Method.TRACE);
    }

    public static HttpRequest of(String str) {
        return of(str, HttpGlobalConfig.isDecodeUrl() ? DEFAULT_CHARSET : null);
    }

    public static HttpRequest of(String str, Charset charset) {
        return of(UrlBuilder.ofHttp(str, charset));
    }

    public static HttpRequest of(UrlBuilder urlBuilder) {
        return new HttpRequest(urlBuilder);
    }

    public static void setGlobalTimeout(int i) {
        HttpGlobalConfig.setTimeout(i);
    }

    public static CookieManager getCookieManager() {
        return GlobalCookieManager.getCookieManager();
    }

    public static void setCookieManager(CookieManager cookieManager) {
        GlobalCookieManager.setCookieManager(cookieManager);
    }

    public static void closeCookie() {
        GlobalCookieManager.setCookieManager((CookieManager) null);
    }

    @Deprecated
    public HttpRequest(String str) {
        this(UrlBuilder.ofHttp(str));
    }

    public HttpRequest(UrlBuilder urlBuilder) {
        this.config = HttpConfig.create();
        this.method = Method.GET;
        this.url = (UrlBuilder) Assert.notNull(urlBuilder, "URL must be not null!", new Object[0]);
        Charset charset = urlBuilder.getCharset();
        if (charset != null) {
            charset(charset);
        }
        header(GlobalHeaders.INSTANCE.headers);
    }

    public String getUrl() {
        return this.url.toString();
    }

    public HttpRequest setUrl(String str) {
        return setUrl(UrlBuilder.ofHttp(str, this.charset));
    }

    public HttpRequest setUrl(UrlBuilder urlBuilder) {
        this.url = urlBuilder;
        return this;
    }

    public HttpRequest setUrlHandler(URLStreamHandler uRLStreamHandler) {
        this.urlHandler = uRLStreamHandler;
        return this;
    }

    public Method getMethod() {
        return this.method;
    }

    public HttpRequest setMethod(Method method) {
        return method(method);
    }

    public HttpConnection getConnection() {
        return this.httpConnection;
    }

    public HttpRequest method(Method method) {
        this.method = method;
        return this;
    }

    public HttpRequest contentType(String str) {
        header(Header.CONTENT_TYPE, str);
        return this;
    }

    public HttpRequest keepAlive(boolean z) {
        header(Header.CONNECTION, z ? "Keep-Alive" : "Close");
        return this;
    }

    public boolean isKeepAlive() {
        String header = header(Header.CONNECTION);
        if (header == null) {
            return !"HTTP/1.0".equalsIgnoreCase(this.httpVersion);
        }
        return !"close".equalsIgnoreCase(header);
    }

    public String contentLength() {
        return header(Header.CONTENT_LENGTH);
    }

    public HttpRequest contentLength(int i) {
        header(Header.CONTENT_LENGTH, String.valueOf(i));
        return this;
    }

    public HttpRequest cookie(Collection<HttpCookie> collection) {
        return cookie(CollUtil.isEmpty(collection) ? null : (HttpCookie[]) collection.toArray(new HttpCookie[0]));
    }

    public HttpRequest cookie(HttpCookie... httpCookieArr) {
        if (ArrayUtil.isEmpty(httpCookieArr)) {
            return disableCookie();
        }
        return cookie(ArrayUtil.join(httpCookieArr, "; "));
    }

    public HttpRequest cookie(String str) {
        this.cookie = str;
        return this;
    }

    public HttpRequest disableCookie() {
        return cookie("");
    }

    public HttpRequest enableDefaultCookie() {
        return cookie((String) null);
    }

    public HttpRequest form(String str, Object obj) {
        String str2;
        if (StrUtil.isBlank(str) || ObjectUtil.isNull(obj)) {
            return this;
        }
        this.body = null;
        if (obj instanceof File) {
            return form(str, (File) obj);
        }
        if (obj instanceof Resource) {
            return form(str, (Resource) obj);
        }
        if (obj instanceof Iterable) {
            str2 = CollUtil.join((Iterable) obj, ",");
        } else if (ArrayUtil.isArray(obj)) {
            if (File.class == ArrayUtil.getComponentType(obj)) {
                return form(str, (File[]) obj);
            }
            str2 = ArrayUtil.join((Object[]) obj, ",");
        } else {
            str2 = Convert.toStr(obj, (String) null);
        }
        return putToForm(str, str2);
    }

    public HttpRequest form(String str, Object obj, Object... objArr) {
        form(str, obj);
        for (int i = 0; i < objArr.length; i += 2) {
            form(objArr[i].toString(), objArr[i + 1]);
        }
        return this;
    }

    public HttpRequest form(Map<String, Object> map) {
        if (MapUtil.isNotEmpty(map)) {
            map.forEach(new -$.Lambda.xu_SqMw4xANmjTAfnZNFF35rprU(this));
        }
        return this;
    }

    public HttpRequest formStr(Map<String, String> map) {
        if (MapUtil.isNotEmpty(map)) {
            map.forEach(new -$.Lambda.tMDzspfRd_RyTqoA9CuYPuTliNg(this));
        }
        return this;
    }

    public HttpRequest form(String str, File... fileArr) {
        if (ArrayUtil.isEmpty(fileArr)) {
            return this;
        }
        if (1 == fileArr.length) {
            File file = fileArr[0];
            return form(str, file, file.getName());
        }
        return form(str, (Resource) new MultiFileResource(fileArr));
    }

    public HttpRequest form(String str, File file) {
        return form(str, file, file.getName());
    }

    public HttpRequest form(String str, File file, String str2) {
        if (file != null) {
            form(str, (Resource) new FileResource(file, str2));
        }
        return this;
    }

    public HttpRequest form(String str, byte[] bArr, String str2) {
        if (bArr != null) {
            form(str, (Resource) new BytesResource(bArr, str2));
        }
        return this;
    }

    public HttpRequest form(String str, Resource resource) {
        if (resource != null) {
            if (!isKeepAlive()) {
                keepAlive(true);
            }
            this.isMultiPart = true;
            return putToForm(str, resource);
        }
        return this;
    }

    public Map<String, Object> form() {
        return this.form;
    }

    public Map<String, Resource> fileForm() {
        HashMap newHashMap = MapUtil.newHashMap();
        this.form.forEach(new -$.Lambda.HttpRequest.SVKJQDNwwLSGxAmayAA9RgNk6uE(newHashMap));
        return newHashMap;
    }

    static void lambda$fileForm$0(Map map, String str, Object obj) {
        if (obj instanceof Resource) {
            map.put(str, (Resource) obj);
        }
    }

    public HttpRequest body(String str) {
        return body(str, null);
    }

    public HttpRequest body(String str, String str2) {
        byte[] bytes = StrUtil.bytes(str, this.charset);
        body(bytes);
        this.form = null;
        if (str2 != null) {
            contentType(str2);
        } else {
            str2 = HttpUtil.getContentTypeByRequestBody(str);
            if (str2 != null && ContentType.isDefault(header(Header.CONTENT_TYPE))) {
                if (this.charset != null) {
                    str2 = ContentType.build(str2, this.charset);
                }
                contentType(str2);
            }
        }
        if (StrUtil.containsAnyIgnoreCase(str2, new CharSequence[]{"json", "xml"})) {
            this.isRest = true;
            contentLength(bytes.length);
        }
        return this;
    }

    public HttpRequest body(byte[] bArr) {
        return ArrayUtil.isNotEmpty(bArr) ? body((Resource) new BytesResource(bArr)) : this;
    }

    public HttpRequest body(Resource resource) {
        if (resource != null) {
            this.body = resource;
        }
        return this;
    }

    public HttpRequest setConfig(HttpConfig httpConfig) {
        this.config = httpConfig;
        return this;
    }

    public HttpRequest timeout(int i) {
        this.config.timeout(i);
        return this;
    }

    public HttpRequest setConnectionTimeout(int i) {
        this.config.setConnectionTimeout(i);
        return this;
    }

    public HttpRequest setReadTimeout(int i) {
        this.config.setReadTimeout(i);
        return this;
    }

    public HttpRequest disableCache() {
        this.config.disableCache();
        return this;
    }

    public HttpRequest setFollowRedirects(boolean z) {
        if (z) {
            if (this.config.maxRedirectCount <= 0) {
                return setMaxRedirectCount(2);
            }
        } else if (this.config.maxRedirectCount < 0) {
            return setMaxRedirectCount(0);
        }
        return this;
    }

    public HttpRequest setFollowRedirectsCookie(boolean z) {
        this.config.setFollowRedirectsCookie(z);
        return this;
    }

    public HttpRequest setMaxRedirectCount(int i) {
        this.config.setMaxRedirectCount(i);
        return this;
    }

    public HttpRequest setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.config.setHostnameVerifier(hostnameVerifier);
        return this;
    }

    public HttpRequest setHttpProxy(String str, int i) {
        this.config.setHttpProxy(str, i);
        return this;
    }

    public HttpRequest setProxy(Proxy proxy) {
        this.config.setProxy(proxy);
        return this;
    }

    public HttpRequest setSSLSocketFactory(SSLSocketFactory sSLSocketFactory) {
        this.config.setSSLSocketFactory(sSLSocketFactory);
        return this;
    }

    public HttpRequest setSSLProtocol(String str) {
        this.config.setSSLProtocol(str);
        return this;
    }

    public HttpRequest setRest(boolean z) {
        this.isRest = z;
        return this;
    }

    public HttpRequest setChunkedStreamingMode(int i) {
        this.config.setBlockSize(i);
        return this;
    }

    public HttpRequest addInterceptor(HttpInterceptor<HttpRequest> httpInterceptor) {
        return addRequestInterceptor(httpInterceptor);
    }

    public HttpRequest addRequestInterceptor(HttpInterceptor<HttpRequest> httpInterceptor) {
        this.config.addRequestInterceptor(httpInterceptor);
        return this;
    }

    public HttpRequest addResponseInterceptor(HttpInterceptor<HttpResponse> httpInterceptor) {
        this.config.addResponseInterceptor(httpInterceptor);
        return this;
    }

    public HttpResponse execute() {
        return execute(false);
    }

    public HttpResponse executeAsync() {
        return execute(true);
    }

    public HttpResponse execute(boolean z) {
        return doExecute(z, this.config.requestInterceptors, this.config.responseInterceptors);
    }

    public void then(Consumer<HttpResponse> consumer) {
        HttpResponse execute = execute(true);
        try {
            consumer.accept(execute);
            if (execute != null) {
                execute.close();
            }
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                if (execute != null) {
                    try {
                        execute.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                }
                throw th2;
            }
        }
    }

    public <T> T thenFunction(Function<HttpResponse, T> function) {
        HttpResponse execute = execute(true);
        try {
            T apply = function.apply(execute);
            if (execute != null) {
                execute.close();
            }
            return apply;
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                if (execute != null) {
                    try {
                        execute.close();
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                    }
                }
                throw th2;
            }
        }
    }

    public HttpRequest basicAuth(String str, String str2) {
        return auth(HttpUtil.buildBasicAuth(str, str2, this.charset));
    }

    public HttpRequest basicProxyAuth(String str, String str2) {
        return proxyAuth(HttpUtil.buildBasicAuth(str, str2, this.charset));
    }

    public HttpRequest bearerAuth(String str) {
        return auth("Bearer " + str);
    }

    public HttpRequest auth(String str) {
        header(Header.AUTHORIZATION, str, true);
        return this;
    }

    public HttpRequest proxyAuth(String str) {
        header(Header.PROXY_AUTHORIZATION, str, true);
        return this;
    }

    public String toString() {
        StringBuilder builder = StrUtil.builder();
        builder.append("Request Url: ").append(this.url.setCharset(this.charset)).append("\r\n");
        builder.append("Request Headers: ").append("\r\n");
        for (Map.Entry entry : this.headers.entrySet()) {
            builder.append("    ").append((String) entry.getKey()).append(": ").append(CollUtil.join((Iterable) entry.getValue(), ",")).append("\r\n");
        }
        builder.append("Request Body: ").append("\r\n");
        builder.append("    ").append(createBody()).append("\r\n");
        return builder.toString();
    }

    private HttpResponse doExecute(boolean z, HttpInterceptor.Chain<HttpRequest> chain, HttpInterceptor.Chain<HttpResponse> chain2) {
        if (chain != null) {
            Iterator it = chain.iterator();
            while (it.hasNext()) {
                ((HttpInterceptor) it.next()).process(this);
            }
        }
        urlWithParamIfGet();
        initConnection();
        send();
        HttpResponse sendRedirectIfPossible = sendRedirectIfPossible(z);
        if (sendRedirectIfPossible == null) {
            sendRedirectIfPossible = new HttpResponse(this.httpConnection, this.config, this.charset, z, isIgnoreResponseBody());
        }
        if (chain2 != null) {
            Iterator it2 = chain2.iterator();
            while (it2.hasNext()) {
                ((HttpInterceptor) it2.next()).process(sendRedirectIfPossible);
            }
        }
        return sendRedirectIfPossible;
    }

    private void initConnection() {
        HttpConnection httpConnection = this.httpConnection;
        if (httpConnection != null) {
            httpConnection.disconnectQuietly();
        }
        HttpConnection header = HttpConnection.create(this.url.setCharset(this.charset).toURL(this.urlHandler), this.config.proxy).setConnectTimeout(this.config.connectionTimeout).setReadTimeout(this.config.readTimeout).setMethod(this.method).setHttpsInfo(this.config.hostnameVerifier, this.config.ssf).setInstanceFollowRedirects(false).setChunkedStreamingMode(this.config.blockSize).header(this.headers, true);
        this.httpConnection = header;
        String str = this.cookie;
        if (str != null) {
            header.setCookie(str);
        } else {
            GlobalCookieManager.add(header);
        }
        if (this.config.isDisableCache) {
            this.httpConnection.disableCache();
        }
    }

    private void urlWithParamIfGet() {
        if (!Method.GET.equals(this.method) || this.isRest || this.redirectCount > 0) {
            return;
        }
        UrlQuery query = this.url.getQuery();
        if (query == null) {
            query = new UrlQuery();
            this.url.setQuery(query);
        }
        if (this.body != null) {
            query.parse(StrUtil.str(this.body.readBytes(), this.charset), this.charset);
        } else {
            query.addAll(this.form);
        }
    }

    private HttpResponse sendRedirectIfPossible(boolean z) {
        UrlBuilder ofHttpWithoutEncode;
        String str;
        String str2;
        if (this.config.maxRedirectCount > 0) {
            try {
                int responseCode = this.httpConnection.responseCode();
                if (this.config.followRedirectsCookie) {
                    GlobalCookieManager.store(this.httpConnection);
                }
                if (responseCode != 200 && HttpStatus.isRedirected(responseCode)) {
                    String header = this.httpConnection.header(Header.LOCATION);
                    if (!HttpUtil.isHttp(header) && !HttpUtil.isHttps(header)) {
                        if (!header.startsWith("/")) {
                            header = StrUtil.addSuffixIfNot(this.url.getPathStr(), "/") + header;
                        }
                        List split = StrUtil.split(header, '?', 2);
                        if (split.size() == 2) {
                            str = (String) split.get(0);
                            str2 = (String) split.get(1);
                        } else {
                            str = header;
                            str2 = null;
                        }
                        ofHttpWithoutEncode = UrlBuilder.of(this.url.getScheme(), this.url.getHost(), this.url.getPort(), str, str2, (String) null, this.charset);
                    } else {
                        ofHttpWithoutEncode = UrlBuilder.ofHttpWithoutEncode(header);
                    }
                    setUrl(ofHttpWithoutEncode);
                    if (this.redirectCount < this.config.maxRedirectCount) {
                        this.redirectCount++;
                        return doExecute(z, this.config.interceptorOnRedirect ? this.config.requestInterceptors : null, this.config.interceptorOnRedirect ? this.config.responseInterceptors : null);
                    }
                }
            } catch (IOException e) {
                this.httpConnection.disconnectQuietly();
                throw new HttpException(e);
            }
        }
        return null;
    }

    private void send() throws IORuntimeException {
        try {
            if (!Method.POST.equals(this.method) && !Method.PUT.equals(this.method) && !Method.DELETE.equals(this.method) && !this.isRest) {
                this.httpConnection.connect();
                return;
            }
            if (isMultipart()) {
                sendMultipart();
            } else {
                sendFormUrlEncoded();
            }
        } catch (IOException e) {
            this.httpConnection.disconnectQuietly();
            throw new IORuntimeException(e);
        }
    }

    private void sendFormUrlEncoded() throws IOException {
        if (StrUtil.isBlank(header(Header.CONTENT_TYPE))) {
            this.httpConnection.header(Header.CONTENT_TYPE, ContentType.FORM_URLENCODED.toString(this.charset), true);
        }
        createBody().writeClose(this.httpConnection.getOutputStream());
    }

    private RequestBody createBody() {
        if (this.body != null) {
            return ResourceBody.create(this.body);
        }
        return FormUrlEncodedBody.create(this.form, this.charset);
    }

    private void sendMultipart() throws IOException {
        ResourceBody create;
        if (this.form == null && this.body != null) {
            create = ResourceBody.create(this.body);
        } else {
            create = MultipartBody.create(this.form, this.charset);
            this.httpConnection.header(Header.CONTENT_TYPE, create.getContentType(), true);
        }
        create.writeClose(this.httpConnection.getOutputStream());
    }

    private boolean isIgnoreResponseBody() {
        return Method.HEAD == this.method || Method.CONNECT == this.method || Method.TRACE == this.method;
    }

    private boolean isMultipart() {
        if (this.isMultiPart) {
            return true;
        }
        String header = header(Header.CONTENT_TYPE);
        return StrUtil.isNotEmpty(header) && header.startsWith(ContentType.MULTIPART.getValue());
    }

    private HttpRequest putToForm(String str, Object obj) {
        if (str != null && obj != null) {
            if (this.form == null) {
                this.form = new TableMap(16);
            }
            this.form.put(str, obj);
        }
        return this;
    }
}
