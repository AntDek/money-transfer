package antdek.moneytransfer

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import antdek.moneytransfer.AccountState.{Balance, Commit, GetBalance, Proposal}
import antdek.moneytransfer.AccountsStateService.{AccountCommand, ActorFactoryException}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._


class AccountsStateServiceTest extends ActorTestKit with Matchers with ScalaFutures{

  implicit val timeout = Timeout(1 second)
  implicit val ex = system.dispatcher

  "Accounts State Service" must {

    val defaultBalance = Balance(100, 1)

    "return balance of the account" in {
      val command = AccountCommand(1, GetBalance)
      newAccountsStateActor ! command
      expectMsg(defaultBalance)
    }

    "change balance of account" in {
      val accountId = 2
      val newBalance = Balance(50, 2)
      val transactionId = "transactionId"

      val proposal = AccountCommand(accountId, Proposal(newBalance.amount, 1, transactionId))
      val commit = AccountCommand(accountId, Commit(transactionId))
      val getBalance = AccountCommand(accountId, GetBalance)

      val actor = newAccountsStateActor

      val futureBalance = for {
        _ <- actor ? proposal
        _ <- actor ? commit
        balance <- actor ? getBalance
      } yield balance

      whenReady(futureBalance) { b =>
        b should be (newBalance)
      }
    }

    "failed if account id doesn't exist" in {
      val command = AccountCommand(500, GetBalance)
      val f = newAccountsStateActor ? command
      whenReady(f.failed) { e =>
        e shouldBe an [ActorFactoryException]
      }
    }
  }

  private def newAccountsStateActor = {
    system.actorOf(Props[AccountsStateService])
  }
}
