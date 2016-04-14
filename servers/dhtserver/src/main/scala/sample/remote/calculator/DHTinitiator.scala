package sample.remote.calculator

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.util.Random
import akka.actor.ActorSystem
import akka.actor.Props

object DHTinitiator {
  def main(args: Array[String]): Unit = {
      startDHTServerSystem()
  }

  def startDHTServerSystem(): Unit = {
    val system = ActorSystem("DHTSystem", ConfigFactory.load("DHTservers"))
    println("Started DHTSystem")
  }

}
