package sample.remote.clients

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random
import akka.actor._
import akka.actor.Props

object Clientinitiator {
  def main(args: Array[String]): Unit = {
    startClientSystem()
  }

  def startClientSystem(): Unit = {
    val system = ActorSystem("ClientSystem", ConfigFactory.load("clients"))

    val oneClient = system.actorOf(Props(classOf[clientActor]), "client1")
    oneClient ! startup()
  }
}

