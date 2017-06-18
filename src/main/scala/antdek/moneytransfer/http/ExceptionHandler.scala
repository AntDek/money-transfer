package antdek.moneytransfer.http

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import antdek.moneytransfer.account.AccountStateActor.TransactionException
import antdek.moneytransfer.transaction.MoneyTransferActor.MoneyTransactionException

object ExceptionHandler extends JacksonSupport {

  import akka.http.scaladsl.server
  import akka.http.scaladsl.model.StatusCodes.Conflict

  case class ErrorResponse(code: Int, message: String)

  def exceptionHandler = server.ExceptionHandler {
    case e: TransactionException =>
      complete(jsonResponse(ErrorResponse(409,e.getClass.getName)))
    case e: MoneyTransactionException =>
      complete(jsonResponse(ErrorResponse(409,e.getClass.getName)))
  }

  private def jsonResponse(error: ErrorResponse): HttpResponse = {
    HttpResponse(Conflict, entity = HttpEntity(`application/json`, Json.generate(error).getBytes("UTF-8")))
  }
}

