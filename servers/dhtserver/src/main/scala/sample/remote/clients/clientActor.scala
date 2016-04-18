package sample.remote.clients

import akka.actor.Props
import akka.actor._
import scala.collection.mutable._
import sample.remote.servers.clientAskForNodes

class clientActor extends Actor {

  private var DHTNodeList = Set[node]()
  val DHTMaster = context.actorSelection("akka.tcp://DHTservers@127.0.0.1:2552/user/MasterActor")

  def receive = {
    case startup() =>
      DHTMaster ! clientAskForNodes






    case lookupNodeGetReturn(key:String,value:Any) =>
      println("The key-value pair is " + key + " - " + value)

    case DHTNodeListReturn(nodeList:Set[node]) =>
      DHTNodeList = nodeList

  }
}
