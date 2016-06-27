package colossus
package protocols.http

import colossus.metrics.TagMap
import core.{InitContext, Server, ServerContext, ServerRef, WorkerRef}
import controller._
import service._

class HttpServiceHandler(rh: RequestHandler, defaultHeaders: HttpHeaders) 
extends BasicServiceHandler[Http](rh) {

  val codec = new StaticHttpServerCodec(defaultHeaders)

  val defaults = new Http.ServerDefaults

  override def tagDecorator = new ReturnCodeTagDecorator[Http]

  override def processRequest(input: Http#Input): Callback[Http#Output] = {
    val response = super.processRequest(input)
    if(!input.head.persistConnection) disconnect()
    response
  }
  def unhandledError = {
    case error => defaults.errorResponse(error)
  }

  //def receivedMessage(message: Any, sender: akka.actor.ActorRef){}

}


abstract class Initializer(context: InitContext) {
  
  implicit val worker = context.worker

  val DateHeader = new DateHeader
  val ServerHeader = HttpHeader("Server", context.server.name.idString)

  val defaultHeaders = HttpHeaders(DateHeader, ServerHeader)

  def onConnect : ServerContext => RequestHandler

}

abstract class RequestHandler(config: ServiceConfig, ctx: ServerContext) extends GenRequestHandler[Http](config, ctx) {
  def this(ctx: ServerContext) = this(ServiceConfig.load(ctx.name), ctx)
}

object HttpServer {
  
  def start(name: String, port: Int)(init: InitContext => Initializer)(implicit io: IOSystem): ServerRef = {
    Server.start(name, port){i => new core.Initializer(i) {
      val httpInitializer = init(i)
      def onConnect = ctx => new HttpServiceHandler(httpInitializer.onConnect(ctx), httpInitializer.defaultHeaders)
    }}
  }

  def basic(name: String, port: Int)(handler: PartialFunction[HttpRequest, Callback[HttpResponse]])(implicit io: IOSystem) = start(name, port){new Initializer(_) {
    def onConnect = new RequestHandler(_) { def handle = handler }
  }}
}

