package antdek.moneytransfer.account

import akka.actor.Actor
import akka.actor.Status.Failure
import antdek.moneytransfer.account.AccountStateActor._


object AccountStateActor {

  sealed trait AccountStateCommand
  case class Proposal(value: BigDecimal, version: Int, transactionId: String) extends AccountStateCommand
  case class Commit(transactionId: String) extends AccountStateCommand
  case class Rollback(transactionId: String) extends AccountStateCommand
  case object GetBalance extends AccountStateCommand


  sealed trait AccountStateCommandResponse
  case object ProposalAccepted extends AccountStateCommandResponse
  case object CommitApplied extends AccountStateCommandResponse
  case object RollbackApplied extends AccountStateCommandResponse

  sealed trait TransactionException extends Exception
  case object TransactionAborted extends TransactionException
  case object InvalidVersion extends TransactionException
  case object InvalidTransactionId extends TransactionException
  case object InTransaction extends TransactionException
}

class AccountStateActor(accountState: AccountState) extends Actor {

  override def receive: Receive = {
    case p: Proposal => sender ! checkNotInTransaction {
      if (!accountState.isVersionMatched(p.version)) {
        Failure(InvalidVersion)
      } else {
        accountState.acceptProposal(p)
        ProposalAccepted
      }
    }

    case c: Commit => sender ! checkInTransaction(c.transactionId) {
      accountState.acceptCommit(c)
      CommitApplied
    }

    case r: Rollback => sender ! checkInTransaction(r.transactionId) {
      accountState.acceptRollback()
      RollbackApplied
    }

    case GetBalance => sender ! accountState.getBalance
  }

  private def checkNotInTransaction(onSuccess: => Any): Any = {
    if (!accountState.isInTransaction)
      onSuccess
    else
      InTransaction
  }

  private def checkInTransaction(transactionId: String)(onSuccess: => Any): Any = {
    if (accountState.isInTransaction(transactionId))
      onSuccess
    else
      Failure(InvalidTransactionId)
  }
}
