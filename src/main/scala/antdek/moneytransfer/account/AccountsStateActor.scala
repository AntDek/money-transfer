package antdek.moneytransfer.account

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import antdek.moneytransfer.account.AccountState.Balance
import antdek.moneytransfer.account.AccountStateActor.AccountStateCommand
import antdek.moneytransfer.account.AccountsStateActor.{AccountCommand, AccountNotFound, ActorFactoryException}

import scala.concurrent.Future


object AccountsStateActor {
  case class AccountCommand(accountId: Int, command: AccountStateCommand)

  sealed trait ActorFactoryException extends Exception
  case object AccountNotFound extends ActorFactoryException
}

class AccountsStateActor(balances: Map[Int, Balance]) extends Actor {

  import context.dispatcher
  import concurrent.duration._

  implicit val timeout = Timeout(5.seconds)

  override def receive: Receive = {
    case c: AccountCommand =>
      val futureCommand = accountActor(c.accountId) match {
        case Left(ex) => Future.failed(ex)
        case Right(actor) => actor ? c.command
      }
      futureCommand pipeTo sender
  }

  private def accountActor(accountId: Int): Either[ActorFactoryException, ActorRef] = {
    val actorName = getActorName(accountId)
    context.child(actorName).map(Right(_)).getOrElse {
      balances.get(accountId)
        .map(balance => Right(createActor(balance, actorName)))
        .getOrElse(Left(AccountNotFound))
    }
  }

  private def createActor(balance: Balance, actorName: String): ActorRef = {
    context.actorOf(
      Props(classOf[AccountStateActor], new AccountState(balance)),
      name = actorName
    )
  }

  private def getActorName(accountId: Int): String = {
    "account-" + accountId
  }
}
