package sample.remote.clients
import scala.collection.mutable._
import akka.actor._
import sample.remote.servers.node

/**
  * Created by Chaoren on 4/17/16.
  */

sealed trait clientAPI
case class startup() extends clientAPI
case class lookupNodeGetReturn(key:String, value:Any) extends clientAPI
case class DHTNodeListReturn(nodeList:Set[node]) extends clientAPI

// for testing
case class DHTTopology(topology:HashMap[node,HashMap[String,Any]])
