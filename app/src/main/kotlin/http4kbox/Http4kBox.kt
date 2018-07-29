package http4kbox

import io.github.konfigur8.Configuration
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.with
import org.http4k.lens.MultipartFormFile
import org.http4k.lens.Path
import org.http4k.lens.Validator.Strict
import org.http4k.lens.multipartForm
import org.http4k.lens.string
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

data class ListFiles(val files: List<S3Key>) : ViewModel

object Index {
    private val templates = HandlebarsTemplates().CachingClasspath()
    private val htmlBody = Body.string(TEXT_HTML).toLens()

    operator fun invoke(s3: S3) = { _: Request ->
        Response(OK).with(htmlBody of templates(ListFiles(s3.list())))
    }
}

object Upload {
    private val files = MultipartFormFile.multi.required("file")
    private val form = Body.multipartForm(Strict, files).toLens()

    operator fun invoke(s3: S3): HttpHandler = { req ->
        files(form(req)).forEach { s3[S3Key(it.filename)] = it.content }
        Response(SEE_OTHER).header("location", "/")
    }
}

object Get {
    private val id = Path.map(::S3Key).of("id")

    operator fun invoke(s3: S3) = { req: Request ->
        s3[id(req)]?.let { Response(OK).header("Content-Type", "application/octet-stream").body(it) } ?: Response(NOT_FOUND)
    }
}

object Delete {
    private val id = Path.map(::S3Key).of("id")

    operator fun invoke(s3: S3) = { req: Request ->
        s3.delete(id(req))
        Response(SEE_OTHER).header("location", "/")
    }
}

object Http4kBox {
    operator fun invoke(config: Configuration, s3Http: HttpHandler): RoutingHttpHandler {
        val s3 = S3.configured(config, s3Http)
        return routes(
                "/{id}/delete" bind POST to Delete(s3),
                "/{id}" bind GET to Get(s3),
                "/" bind routes(
                        POST to Upload(s3),
                        GET to Index(s3)
                )
        )
    }
}