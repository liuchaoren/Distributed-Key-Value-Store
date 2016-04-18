package sample.remote.clients

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random
import akka.actor._
import akka.actor.Props

object clientInitiator {
  def main(args: Array[String]): Unit = {
    startClientSystem()
  }

  def startClientSystem(): Unit = {
    val system = ActorSystem("ClientSystem", ConfigFactory.load("clients"))
    println("Clients system was started")
    val remoteDHTMasterPath = "akka.tcp://DHTservers@127.0.0.1:2552/user/MasterActor"
    val oneClient = system.actorOf(Props(classOf[clientActor], remoteDHTMasterPath), "client1")
    import system.dispatcher
    system.scheduler.scheduleOnce(3 seconds) {
      oneClient ! startup()
    }

  }
}

