package antdek.moneytransfer

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import antdek.moneytransfer.AccountState.{Balance, AccountStateCommand}
import antdek.moneytransfer.AccountsStateService.{AccountCommand, AccountNotFound, ActorFactoryException}

import scala.concurrent.Future


object AccountsStateService {

  case class AccountCommand(accountId: Int, command: AccountStateCommand)

  sealed trait ActorFactoryException extends Exception
  case object AccountNotFound extends ActorFactoryException
}

class AccountsStateService extends Actor {

  import concurrent.duration._
  import context.dispatcher
  implicit val timeout = Timeout(4.seconds)

  private val defaultBalances = List(
    Balance(100, 1),
    Balance(100, 1),
    Balance(100, 1),
    Balance(100, 1),
    Balance(100, 1)
  )

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
      if (accountId < defaultBalances.size)
        Right(createActor(accountId, actorName))
      else
        Left(AccountNotFound)
    }
  }

  private def createActor(accountId: Int, actorName: String): ActorRef = {
    context.actorOf(Props(classOf[AccountState], accountId.toString, defaultBalances(accountId)), name = actorName)
  }

  private def getActorName(accountId: Int): String = {
    "account-" + accountId
  }
}
