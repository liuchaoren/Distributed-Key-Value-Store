package sample.remote.clients
import scala.collection.mutable._
import akka.actor._

/**
  * Created by Chaoren on 4/17/16.
  */
case class node(path:ActorPath,nameHash:BigInt,actorNode:ActorRef)

sealed trait clientAPI
case class startup() extends clientAPI
case class lookupNodeGetReturn(key:String, value:Any) extends clientAPI
case class DHTNodeListReturn(nodeList:Set[node]) extends clientAPI

