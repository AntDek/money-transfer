package antdek.moneytransfer.transaction

import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountStateActor.{Commit, Proposal, Rollback}
import antdek.moneytransfer.account.AccountsStateActor.AccountCommand
import antdek.moneytransfer.transaction.MoneyTransferActor.MoneyTransaction

class MoneyTransfer(transaction: MoneyTransaction, fromBalance: Balance, toBalance: Balance, transactionId: String) {

  import transaction.{fromAccount, toAccount, amount => transactionAmount}

  def canTransferMoney: Boolean = {
    fromBalance.amount - transactionAmount >= 0
  }

  def createProposals: List[AccountCommand] = List(
    AccountCommand(fromAccount, Proposal(fromBalance.amount - transactionAmount, fromBalance.version, transactionId)),
    AccountCommand(toAccount, Proposal(toBalance.amount + transactionAmount, toBalance.version, transactionId))
  )

  def createCommits: List[AccountCommand] = List(
    AccountCommand(fromAccount, Commit(transactionId)),
    AccountCommand(toAccount, Commit(transactionId))
  )

  def createRollbacks: List[AccountCommand] = List(
    AccountCommand(fromAccount, Rollback(transactionId)),
    AccountCommand(toAccount, Rollback(transactionId))
  )
}
