package http4kbox

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.serverless.AppLoader


object Http4kboxLambda : AppLoader {
    override fun invoke(env: Map<String, String>): HttpHandler {
        env.forEach { System.setProperty(it.key, it.value)}
        return Http4kBox(Settings.defaults.reify(), JavaHttpClient())
    }
}