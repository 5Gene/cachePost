package osp.sparkj.cachepost

import okhttp3.*
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

const val masks = "masks_get"
const val forceRefresh = "force_refresh"

/**
 * 还原为 POST 允许正常请求接口
 * 接口访问后 将响应伪装为 GET 允许接下来的 CacheInterceptor缓存响应
 */
class PostRequestGetResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val isNeedRestore = !request.header(masks).isNullOrEmpty()
        val requestBody = request.body!!
        if (isNeedRestore) {
            //CacheInterceptor没使用缓存后需要请求网络，将伪装的get请求 还原为post 正常请求网络
            val queryIndex = request.url.toString().indexOf("?")
            request = request.newBuilder()
                .removeHeader(masks)
                .removeHeader(forceRefresh)
                .url(request.url.toString().substring(0, queryIndex))
                .method("POST", requestBody)
                .build()
        }

        var response = chain.proceed(request)

        if (isNeedRestore) {
            val reqBuilder = response.request.newBuilder()
                // 把 POST 改成 GET 让接下来的 CacheInterceptor 缓存响应数据
                .url("${request.url}?readCache=${request.body?.read().hashCode()}")
                .method("GET", null)

            response = response.newBuilder()
                .request(reqBuilder.build())
                .addHeader(
                    "Cache-Control", request.header("Cache-Control") ?: CacheControl.Builder()
                        .maxAge(12, TimeUnit.HOURS)
                        .build().toString()
                )
                .build()
        } else {
            response = response.newBuilder()
                .request(response.request.newBuilder().build())
                .addHeader(
                    "Cache-Control", request.header("Cache-Control") ?: CacheControl.Builder()
                        .maxAge(12, TimeUnit.HOURS)
                        .build().toString()
                )
                .build()
        }
        println(" access internet -->>  ${request.url.encodedPath} ")
        return response
    }
}

/**
 * 这里会把所有POST请求 伪装成 GET 请求
 * 目的是 让 CacheInterceptor 去看是否有缓存可用
 */
class AllGetRequestInterceptor : Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        var request = chain.request()

        if (request.method == "POST") {
            //将post请求改成get 尝试等CacheInterceptor读取缓存
            val noReadCache = request.header(forceRefresh) != null
            val url = if (noReadCache) {
                "${request.url}?noReadCache=${System.currentTimeMillis()}"
            } else {
                "${request.url}?readCache=${request.body?.read().hashCode()}"
            }
            val builder = request.newBuilder()
                .method("GET", null)
                .url(url)
                .addHeader(masks, masks)
                .removeHeader(forceRefresh)
                .addHeader(
                    "Cache-Control", request.header("Cache-Control") ?: CacheControl.Builder()
                        .maxAge(12, TimeUnit.HOURS)
                        .build().toString()
                )
            //保存 requestBody (GET请求无法保存body会报错) 等 真正要还原为post调用的时候使用
            saveRequestBody(builder, request.body)
            request = builder.build()
        } else {
            val builder = request.newBuilder()
                .addHeader(
                    "Cache-Control", request.header("Cache-Control") ?: CacheControl.Builder()
                        .maxAge(12, TimeUnit.HOURS)
                        .build().toString()
                )

            request = builder.build()
        }

        val proceed = chain.proceed(request)
        println("AllGetRequestInterceptor -->> ${proceed.cacheResponse}")
        return proceed
    }
}

private fun saveRequestBody(builder: Request.Builder, body: RequestBody?) {
    val bodyField = builder.javaClass.getDeclaredField("body")
    bodyField.isAccessible = true
    bodyField.set(builder, body)
}

private fun RequestBody.read(): String {
    val buffer = okio.Buffer()
    writeTo(buffer)
    return "${buffer.readString(Charset.forName("UTF-8"))}_userid"
}