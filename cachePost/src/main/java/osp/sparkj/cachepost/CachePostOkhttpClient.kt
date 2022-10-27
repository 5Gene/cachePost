import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import osp.sparkj.cachepost.AllGetRequestInterceptor
import osp.sparkj.cachepost.PostRequestGetResponseInterceptor
import java.io.File
import java.util.concurrent.TimeUnit


fun OkHttpClient.Builder.cachePost(): OkHttpClient.Builder = apply {
    addInterceptor(AllGetRequestInterceptor())
    addNetworkInterceptor(PostRequestGetResponseInterceptor())
}

fun okHttpClient(cacheFile: File, moreInterceptors: OkHttpClient.Builder.() -> Unit): OkHttpClient =
    OkHttpClient.Builder()
        .readTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        //这里给OkHttp设置缓存功能
        .cache(Cache(File(cacheFile, "post_cache"), 10L shl 20))
        .apply(moreInterceptors)
        //打印网络请求日志
        .addInterceptor(HttpLoggingInterceptor { print("Retrofit -> $it ") }.also {
            it.level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(AllGetRequestInterceptor())
        .addNetworkInterceptor(PostRequestGetResponseInterceptor())
        .retryOnConnectionFailure(true)
        .build()
