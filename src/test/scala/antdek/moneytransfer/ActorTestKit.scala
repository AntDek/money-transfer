package antdek.moneytransfer

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

abstract class ActorTestKit extends TestKit(ActorSystem("TestAccountService"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}
