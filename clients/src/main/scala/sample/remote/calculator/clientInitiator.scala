package sample.remote.calculator

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random
import akka.actor.ActorSystem
import akka.actor.Props

object Clientinitiator {
  def main(args: Array[String]): Unit = {
    startClientSystem()
  }

  def startClientSystem(): Unit = {
    val system =
      ActorSystem("ClientSystem", ConfigFactory.load("remotecreation"))
    val actor = system.actorOf(Props[DHTActor],
      name = "creationActor")

    println("Started CreationSystem")
    actor ! salutation("hello")
  }
}
