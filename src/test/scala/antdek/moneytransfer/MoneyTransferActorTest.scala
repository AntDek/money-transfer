package antdek.moneytransfer

import akka.actor.Props
import akka.actor.Status.Failure
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountsStateActor
import antdek.moneytransfer.transaction.MoneyTransferActor
import antdek.moneytransfer.transaction.MoneyTransferActor.{MoneyTransaction, NotEnoughMoney, TransactionCompleted}


class MoneyTransferActorTest extends ActorTestKit {

  private val balances = Map(
    1 -> Balance(100, 1),
    2 -> Balance(100, 1)
  )

  "Accounts State Service" must {
    "transfer money between accounts" in {
      val actor = newMoneyTransferActor
      val command = MoneyTransaction(1, 2, 40)

      actor ! command
      expectMsg(TransactionCompleted)
    }

    "fail if not enough money" in {
      val actor = newMoneyTransferActor
      val notEnoughCommand = MoneyTransaction(1, 2, 100.1)

      actor ! notEnoughCommand
      expectMsg(Failure(NotEnoughMoney))
    }
  }

  private def newAccountsStateActor = {
    system.actorOf(Props(classOf[AccountsStateActor], balances))
  }

  private def newMoneyTransferActor = {
    system.actorOf(Props(classOf[MoneyTransferActor], newAccountsStateActor))
  }
}
