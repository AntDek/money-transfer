package antdek.moneytransfer.transaction

import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountStateActor.{Commit, GetBalance, Proposal, Rollback}
import antdek.moneytransfer.account.AccountsStateActor.AccountCommand
import antdek.moneytransfer.transaction.MoneyTransferActor._

import scala.concurrent.Future

object MoneyTransferActor {

  sealed trait MoneyTransactionException extends Exception
  case object NotEnoughMoney extends MoneyTransactionException
  case object TransactionFailed extends MoneyTransactionException

  sealed trait TransactionStatus
  case object TransactionCompleted extends TransactionStatus

  case class MoneyTransaction(fromAccount: Int, toAccount: Int, amount: BigDecimal)
}

class MoneyTransferActor(accountsService: ActorRef) extends Actor {

  import context.dispatcher
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 second)

  private var transactionsCount: Int = 0

  override def receive: Receive = {
    case tr: MoneyTransaction =>
      val transactionId: String = "transaction-" + transactionsCount.toString
      transactionsCount += 1

      val transaction: Future[TransactionStatus] = for {
        moneyTransfer <- createMoneyTransfer(tr, transactionId)
        _ <- sendProposals(moneyTransfer)
        _ <- sendCommits(moneyTransfer)
      } yield TransactionCompleted

      transaction pipeTo sender
  }

  private def createMoneyTransfer(transaction: MoneyTransaction, transactionId: String): Future[MoneyTransfer] = {
    for {
      fromBalance <- (accountsService ? AccountCommand(transaction.fromAccount, GetBalance)).mapTo[Balance]
      toBalance <- (accountsService ? AccountCommand(transaction.toAccount, GetBalance)).mapTo[Balance]
    } yield new MoneyTransfer(transaction, fromBalance, toBalance, transactionId)
  }

  private def sendCommits(moneyTransfer: MoneyTransfer) = {
    sendToAccountsService(moneyTransfer.createCommits)
  }

  private def sendProposals(moneyTransfer: MoneyTransfer) = {
    if (!moneyTransfer.canTransferMoney)
      Future.failed(NotEnoughMoney)
    else
      sendToAccountsService(moneyTransfer.createProposals) recoverWith {
        case _ => sendToAccountsService(moneyTransfer.createRollbacks)
          .flatMap(_ => Future.failed(TransactionFailed))
      }
  }

  private def sendToAccountsService(commands: List[AccountCommand]) = {
    Future.sequence(commands.map(command=>accountsService ? command))
  }
}
