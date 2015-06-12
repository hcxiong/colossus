package colossus
package protocols.http

import colossus.core._
import colossus.parsing.DataSize
import colossus.service._
import Codec.ClientCodec
import parsing._
import Combinators.Parser
import DataSize._

class BaseHttpClientCodec[T <: BaseHttpResponse](parserFactory: () => Parser[DecodedResult[T]]) extends ClientCodec[HttpRequest, T] {

  private var parser : Parser[DecodedResult[T]] = parserFactory()


  override def encode(out: HttpRequest): DataReader = DataBuffer(out.bytes)

  override def decode(data: DataBuffer): Option[DecodedResult[T]] = parser.parse(data)

  override def reset(): Unit = { 
    parser = parserFactory()
  }

  override def endOfStream() = parser.endOfStream()

}

class HttpClientCodec(maxResponseSize: DataSize = 1.MB) 
  extends BaseHttpClientCodec[HttpResponse](() => HttpResponseParser.static(maxResponseSize))

class StreamingHttpClientCodec(dechunk: Boolean = false)
  extends BaseHttpClientCodec[StreamingHttpResponse](() => HttpResponseParser.stream(dechunk))

