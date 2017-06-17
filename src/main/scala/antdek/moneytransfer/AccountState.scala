package antdek.moneytransfer

import akka.actor.Actor
import antdek.moneytransfer.AccountState._
import antdek.moneytransfer.AccountState.AccountStateResponse._

object AccountState {

  case class Transaction(id: String, balance: Balance)
  case class Balance(amount: BigDecimal, version: Int)

  case class Proposal(value: BigDecimal, version: Int, transactionId: String)
  case class Commit(transactionId: String)
  case class Rollback(transactionId: String)
  case object GetBalance

  case class AccountStateResponse(state: AccountStateResponse.State)

  object AccountStateResponse {
    sealed trait State
    sealed trait Error extends State
    case object ProposalAccepted extends State
    case object CommitApplied extends State
    case object TransactionAborted extends State
    case object InvalidVersion extends Error
    case object InvalidTransactionId extends Error
    case object ProposalDeclined extends Error
  }
}

class AccountState(accountId: String, private var lastCommitBalance: Balance) extends Actor {

  private var acceptedTransaction: Option[Transaction] = None

  override def receive: Receive = {
    case p: Proposal => checkNotInTransaction {
      if (p.version != lastCommitBalance.version) {
        sender ! AccountStateResponse(InvalidVersion)
      } else {
        val newBalance = Balance(p.value, p.version + 1)
        acceptedTransaction = Some(Transaction(p.transactionId, newBalance))
        sender ! AccountStateResponse(ProposalAccepted)
      }
    }

    case c: Commit => checkInTransaction(c.transactionId) {
      lastCommitBalance = acceptedTransaction.get.balance
      acceptedTransaction = None
      sender ! AccountStateResponse(CommitApplied)
    }

    case r: Rollback => checkInTransaction(r.transactionId) {
      acceptedTransaction = None
      sender ! AccountStateResponse(TransactionAborted)
    }

    case GetBalance => sender ! lastCommitBalance
  }

  private def checkNotInTransaction(onSuccess: => Unit): Unit = {
    if (acceptedTransaction.isEmpty)
      onSuccess
    else
      sender ! AccountStateResponse(ProposalDeclined)
  }

  private def checkInTransaction(transactionId: String)(onSuccess: => Unit): Unit = {
    if (acceptedTransaction.isDefined && acceptedTransaction.get.id == transactionId)
      onSuccess
    else
      sender ! AccountStateResponse(InvalidTransactionId)
  }
}
