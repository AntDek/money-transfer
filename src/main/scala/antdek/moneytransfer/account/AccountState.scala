package antdek.moneytransfer.account

import antdek.moneytransfer.account.AccountState._
import antdek.moneytransfer.account.AccountStateActor.{Commit, Proposal}

object AccountState {
  case class Transaction(id: String, balance: Balance)
  case class Balance(amount: BigDecimal, version: Int)
}

class AccountState(private var lastCommittedBalance: Balance) {

  private var acceptedTransaction: Option[Transaction] = None

  def isVersionMatched(version: Int): Boolean = {
    version == lastCommittedBalance.version
  }

  def acceptProposal(proposal: Proposal): Unit = {
    val newBalance = Balance(proposal.value, proposal.version + 1)
    acceptedTransaction = Some(Transaction(proposal.transactionId, newBalance))
  }

  def acceptCommit(commit: Commit): Unit = {
    lastCommittedBalance = acceptedTransaction.get.balance
    acceptedTransaction = None
  }

  def acceptRollback(): Unit = {
    acceptedTransaction = None
  }

  def isInTransaction: Boolean = {
    acceptedTransaction.isDefined
  }

  def isInTransaction(transactionId: String): Boolean = {
    acceptedTransaction.isDefined && acceptedTransaction.get.id == transactionId
  }

  def getBalance = lastCommittedBalance

}
