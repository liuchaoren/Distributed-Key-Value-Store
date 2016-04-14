package sample.remote.calculator

import akka.actor.Props
import akka.actor.Actor

sealed trait DHTserverAPI
case class salutation(words:String) extends DHTserverAPI
case class shutdown() extends DHTserverAPI

class DHTActor extends Actor {
  def receive = {
    case salutation(words:String) =>
      println("I received salutation from a client!")
    case shutdown() =>
      context.stop(self)
  }
}

