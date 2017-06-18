package antdek.moneytransfer

import akka.actor.Props
import antdek.moneytransfer.AccountState.AccountStateResponse._
import antdek.moneytransfer.AccountState._

class AccountStateTest extends ActorTestKit {

  private val transactionId = "transaction_id"
  private val accountId = "account_id"
  private val defaultBalance = Balance(100, 1)
  private val defaultProposal = Proposal(30, defaultBalance.version, transactionId)

  "Account State" must {

    "provide account balance" in {
      newAccountActor ! GetBalance
      expectMsg(defaultBalance)
    }

    "except proposal" in {
      newAccountActor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalAccepted))
    }

    "commit transaction" in {
      val accountActor = newAccountActor

      accountActor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalAccepted))

      accountActor ! Commit(transactionId)
      expectMsg(AccountStateResponse(CommitApplied))
    }

    "rollback transaction" in {
      val accountActor = newAccountActor

      accountActor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalAccepted))

      accountActor ! Rollback(transactionId)
      expectMsg(AccountStateResponse(TransactionAborted))
    }
  }

  "Account State data consistency" must {
    "reject proposal with invalid balance version" in {
      val invalidVersion = defaultBalance.version + 150
      val proposal = Proposal(3, invalidVersion, transactionId)

      newAccountActor ! proposal
      expectMsg(AccountStateResponse(InvalidVersion))
    }

    "reject proposal if transaction is in progress" in {
      val actor = newAccountActor

      actor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalAccepted))

      actor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalDeclined))
    }

    "reject commit if transaction is incorrect" in {
      val actor = newAccountActor
      val incorrectTransactionIs = "incorrect_transaction"

      actor ! defaultProposal
      expectMsg(AccountStateResponse(ProposalAccepted))

      actor ! Commit(incorrectTransactionIs)
      expectMsg(AccountStateResponse(InvalidTransactionId))
    }
  }

  private def newAccountActor = {
    system.actorOf(Props(classOf[AccountState], accountId, defaultBalance))
  }

}
