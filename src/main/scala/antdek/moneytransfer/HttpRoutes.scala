package antdek.moneytransfer

import akka.actor.ActorRef
import akka.pattern.ask
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import antdek.moneytransfer.HttpRoutes.TransferResponse
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountStateActor.GetBalance
import antdek.moneytransfer.account.AccountsStateActor.AccountCommand
import antdek.moneytransfer.http.JacksonSupport
import antdek.moneytransfer.transaction.MoneyTransferActor.MoneyTransaction

import scala.concurrent.ExecutionContext

object HttpRoutes {
  case class TransferResponse(message: String)
}

class HttpRoutes(moneyTransferActor: ActorRef, accountsStateActor: ActorRef)(implicit ec: ExecutionContext) extends JacksonSupport {
  import akka.http.scaladsl.server.Directives._
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 second)

  def routes: Route = pathPrefix("v1" / "balance") {
    (path("transfer") & post) {
      entity(as[MoneyTransaction]) { transaction =>
        complete(
          (moneyTransferActor ? transaction).map(_ => TransferResponse("Transfer successfully completed"))
        )
      }
    } ~ (path(IntNumber) & get) { accountId =>
      complete(
        (accountsStateActor ? AccountCommand(accountId, GetBalance)).mapTo[Balance]
      )
    }
  }

}
