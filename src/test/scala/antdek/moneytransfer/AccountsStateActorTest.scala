package antdek.moneytransfer

import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountStateActor._
import antdek.moneytransfer.account.AccountsStateActor
import antdek.moneytransfer.account.AccountsStateActor.{AccountCommand, ActorFactoryException}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._


class AccountsStateActorTest extends ActorTestKit with Matchers with ScalaFutures{

  import system.dispatcher
  implicit val timeout = Timeout(5 second)

  private val balances = Map(
    1 -> Balance(100, 1),
    2 -> Balance(100, 1)
  )

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
    system.actorOf(Props(classOf[AccountsStateActor], balances))
  }
}
