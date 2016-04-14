package sample.remote.calculator

import akka.actor.Props
import akka.actor.Actor

sealed trait clientserverAPI

class CalculatorActor extends Actor {
  def receive = {
    case _ =>
      sender() ! "hello"
  }
}