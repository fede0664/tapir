package sttp.tapir.server.play

import akka.stream.Materializer
import play.api.mvc.RequestHeader
import play.utils.UriEncoding
import sttp.model.{Method, QueryParams}
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interpreter.DecodeInputsContext

import java.nio.charset.StandardCharsets

private[play] class PlayDecodeInputContext(request: RequestHeader, pathConsumed: Int = 0, serverOptions: PlayServerOptions)(implicit
    mat: Materializer
) extends DecodeInputsContext {
  override def method: Method = Method(request.method.toUpperCase())

  override def nextPathSegment: (Option[String], DecodeInputsContext) = {
    val path = request.path.drop(pathConsumed)
    val nextStart = path.dropWhile(_ == '/')
    val segment = nextStart.split("/", 2) match {
      case Array("")   => None
      case Array(s)    => Some(s)
      case Array(s, _) => Some(s)
    }
    val charactersConsumed = segment.map(_.length).getOrElse(0) + (path.length - nextStart.length)
    val urlDecodedSegment = segment.map(UriEncoding.decodePathSegment(_, StandardCharsets.UTF_8))

    (urlDecodedSegment, new PlayDecodeInputContext(request, pathConsumed + charactersConsumed, serverOptions))
  }
  override def header(name: String): List[String] = request.headers.toMap.get(name).toList.flatten
  override def headers: Seq[(String, String)] = request.headers.headers
  override def queryParameter(name: String): Seq[String] = request.queryString.get(name).toSeq.flatten
  override def queryParameters: QueryParams = QueryParams.fromMultiMap(request.queryString)
  override def bodyStream: Any = throw new UnsupportedOperationException("Play doesn't support request body streaming")

  override def request: ServerRequest = new PlayServerRequest(request)
}
