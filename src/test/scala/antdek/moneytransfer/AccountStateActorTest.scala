package antdek.moneytransfer

import akka.actor.Props
import akka.actor.Status.Failure
import akka.util.Timeout
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.{AccountState, AccountStateActor}
import antdek.moneytransfer.account.AccountStateActor._

import scala.concurrent.duration._

class AccountStateActorTest extends ActorTestKit {

  implicit val timeout = Timeout(5 second)

  private val transactionId = "transaction_id"
  private val defaultBalance = Balance(100, 1)
  private val defaultProposal = Proposal(30, defaultBalance.version, transactionId)

  "Account State" must {

    "provide account balance" in {
      newAccountActor ! GetBalance
      expectMsg(defaultBalance)
    }

    "except proposal" in {
      newAccountActor ! defaultProposal
      expectMsg(ProposalAccepted)
    }

    "commit transaction" in {
      val accountActor = newAccountActor

      accountActor ! defaultProposal
      expectMsg(ProposalAccepted)

      accountActor ! Commit(transactionId)
      expectMsg(CommitApplied)
    }

    "rollback transaction" in {
      val accountActor = newAccountActor

      accountActor ! defaultProposal
      expectMsg(ProposalAccepted)

      accountActor ! Rollback(transactionId)
      expectMsg(RollbackApplied)
    }
  }

  "Account State data consistency" must {
    "reject proposal with invalid balance version" in {
      val invalidVersion = defaultBalance.version + 150
      val proposal = Proposal(3, invalidVersion, transactionId)

      newAccountActor ! proposal
      expectMsg(Failure(InvalidVersion))
    }

    "reject proposal if transaction is in progress" in {
      val actor = newAccountActor

      actor ! defaultProposal
      expectMsg(ProposalAccepted)

      actor ! defaultProposal
      expectMsg(InTransaction)
    }

    "reject commit if transaction is incorrect" in {
      val actor = newAccountActor
      val incorrectTransactionIs = "incorrect_transaction"

      actor ! defaultProposal
      expectMsg(ProposalAccepted)

      actor ! Commit(incorrectTransactionIs)
      expectMsg(Failure(InvalidTransactionId))
    }
  }

  private def newAccountActor = {
    system.actorOf(Props(classOf[AccountStateActor], new AccountState(defaultBalance)))
  }

}
