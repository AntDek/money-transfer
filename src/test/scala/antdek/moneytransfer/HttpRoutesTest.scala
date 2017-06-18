package antdek.moneytransfer

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import antdek.moneytransfer.HttpRoutes.TransferResponse
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountsStateActor
import antdek.moneytransfer.http.{ExceptionHandler, JacksonSupport}
import antdek.moneytransfer.transaction.MoneyTransferActor
import antdek.moneytransfer.transaction.MoneyTransferActor.MoneyTransaction
import org.scalatest.{Matchers, WordSpec}

class HttpRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with JacksonSupport {

  implicit val exceptionHandler = ExceptionHandler.exceptionHandler

  private val balances = Map(
    1 -> Balance(100, 1),
    2 -> Balance(100, 1)
  )

  "HttpRoutesTest" must {
    "transfer money between accounts" in {
      val actor = newMoneyTransferActor()
      val command = MoneyTransaction(1, 2, 40)
      val routes = new HttpRoutes(actor, null).routes

      Post("/v1/balance/transfer", command) ~> routes ~> check {
        assert(status === StatusCodes.OK)
        responseAs[TransferResponse] shouldBe an [TransferResponse]
      }
    }

    "fail if not enough money" in {
      val actor = newMoneyTransferActor()
      val command = MoneyTransaction(1, 2, 200)
      val routes = new HttpRoutes(actor, null).routes

      Post("/v1/balance/transfer", command) ~> routes ~> check {
        assert(status === StatusCodes.Conflict)
      }
    }

    "return balance of account" in {
      val accountsStateActor = newAccountsStateActor
      val moneyTransferActor = newMoneyTransferActor(accountsStateActor)
      val routes = new HttpRoutes(moneyTransferActor, accountsStateActor).routes

      Get("/v1/balance/1") ~> routes ~> check {
        assert(status === StatusCodes.OK)
        responseAs[Balance] should be (balances(1))
      }
    }
  }

  private def newAccountsStateActor = {
    system.actorOf(Props(classOf[AccountsStateActor], balances))
  }

  private def newMoneyTransferActor(accountsStateActor: ActorRef = newAccountsStateActor) = {
    system.actorOf(Props(classOf[MoneyTransferActor], newAccountsStateActor))
  }
}
