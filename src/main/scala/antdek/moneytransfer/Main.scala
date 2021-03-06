package antdek.moneytransfer

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountsStateActor
import antdek.moneytransfer.http.ExceptionHandler
import antdek.moneytransfer.transaction.MoneyTransferActor

import scala.concurrent.ExecutionContext

object Main extends App {

  implicit val actorSystem = ActorSystem("accounts_balance_system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val exceptionHandler = ExceptionHandler.exceptionHandler

  val httpInterface = "localhost"
  val httpPort = 8080

  val balances: Map[Int, Balance] = (1 to 1000).map {i =>
    i -> Balance(77.96654 * i, 1)
  }.toMap

  val accountsStateActor = actorSystem.actorOf(Props(classOf[AccountsStateActor], balances))
  val moneyTransferActor = actorSystem.actorOf(Props(classOf[MoneyTransferActor], accountsStateActor))

  val apiRoutes: Route = new HttpRoutes(moneyTransferActor, accountsStateActor).routes

  Http().bindAndHandle(apiRoutes, httpInterface, httpPort)
  actorSystem.log.info("Server online on http://{}:{}/", httpInterface, httpPort)

}
