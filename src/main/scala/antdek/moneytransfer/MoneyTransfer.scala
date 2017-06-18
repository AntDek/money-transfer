package antdek.moneytransfer

import akka.actor.{Actor, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import antdek.moneytransfer.AccountState._
import antdek.moneytransfer.AccountsStateService.AccountCommand
import antdek.moneytransfer.MoneyTransfer._

import scala.concurrent.Future

object MoneyTransfer {

  sealed trait MoneyTransactionException extends Exception
  case object NotEnoughMoney extends MoneyTransactionException

  sealed trait TransactionStatus
  case object TransactionCompleted extends TransactionStatus
  case object TransactionFailed extends TransactionStatus

  case class MoneyTransaction(fromAccount: Int, toAccount: Int, amount: BigDecimal)
}

class MoneyTransfer(accountsService: ActorRef) extends Actor {

  import context.dispatcher
  import scala.concurrent.duration._

  implicit val timeout = Timeout(10 second)

  private var transactionsCount: Int = 0

  override def receive: Receive = {
    case tr: MoneyTransaction =>
      val transactionId: String = "transaction-" + transactionsCount.toString
      transactionsCount += 1

      val balances: Future[(Balance, Balance)] = for {
        a <- (accountsService ? AccountCommand(tr.fromAccount, GetBalance)).mapTo[Balance]
        b <- (accountsService ? AccountCommand(tr.toAccount, GetBalance)).mapTo[Balance]
      } yield (a, b)

      val proposals: Future[List[AccountCommand]] = balances.flatMap {
        case (fromBalance, toBalance) =>
          val leftBalance = fromBalance.amount - tr.amount
          if (leftBalance >= 0) {
            val a = AccountCommand(tr.fromAccount, Proposal(fromBalance.amount - tr.amount, fromBalance.version, transactionId))
            val b = AccountCommand(tr.toAccount, Proposal(toBalance.amount + tr.amount, toBalance.version, transactionId))
            Future.successful(List(a, b))
          } else {
            Future.failed(NotEnoughMoney)
          }
      }

      val commits = List(
        AccountCommand(tr.fromAccount, Commit(transactionId)),
        AccountCommand(tr.toAccount, Commit(transactionId))
      )

      val rollbacks = List(
        AccountCommand(tr.fromAccount, Rollback(transactionId)),
        AccountCommand(tr.toAccount, Rollback(transactionId))
      )

      val transaction = for {
        ps <- proposals
        _ <- Future.sequence(ps.map(p=>accountsService ? p))
        _ <- Future.sequence(commits.map(c=>accountsService ? c))
      } yield TransactionCompleted

      transaction recoverWith {
        case _ => Future
          .sequence(rollbacks.map(r=>accountsService ? r))
          .map(_ => TransactionFailed)
      } pipeTo sender

  }
}
