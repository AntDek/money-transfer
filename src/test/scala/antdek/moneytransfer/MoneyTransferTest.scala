package antdek.moneytransfer

import akka.actor.Props
import antdek.moneytransfer.MoneyTransfer.{MoneyTransaction, TransactionCompleted, TransactionFailed}


class MoneyTransferTest extends ActorTestKit {

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
      expectMsg(TransactionFailed)
    }
  }

  private def newAccountsStateActor = {
    system.actorOf(Props[AccountsStateService])
  }

  private def newMoneyTransferActor = {
    system.actorOf(Props(classOf[MoneyTransfer], newAccountsStateActor))
  }
}
