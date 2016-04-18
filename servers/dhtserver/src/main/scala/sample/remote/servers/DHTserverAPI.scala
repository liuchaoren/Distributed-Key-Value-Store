package sample.remote.servers

import akka.actor.{ActorRef,ActorPath}
import scala.collection.mutable.{HashMap,ArraySeq}

/**
  * Created by Chaoren on 4/11/16.
  */

sealed trait DHTserverAPI
case class clientGet(key:String) extends DHTserverAPI
case class clientGetReturn(key:String,value:Any) extends DHTserverAPI
case class clientPut(key:String,value:String) extends DHTserverAPI

//case class salutation(words:String) extends DHTserverAPI
//case class shutdown() extends DHTserverAPI
//case class put(key:BigInt, value:Any) extends DHTserverAPI
//case class get(key:BigInt) extends DHTserverAPI
//case class lookUp(client:ActorRef,key:String,id:String) extends DHTserverAPI
case class lookupForward(key:String, keyhash:BigInt, hostNode:node) extends DHTserverAPI
case class lookupPredecessorFound(key:String,keyhash:BigInt,nodeFound:node) extends DHTserverAPI
case class lookupGetSuccessor(key:String,keyhash:BigInt) extends DHTserverAPI
case class lookupSuccessorFound(key:String,keyhash:BigInt,nodeFound:node) extends DHTserverAPI
case class lookupNodeGet(key:String) extends DHTserverAPI
case class lookupNodeGetReturn(key:String,value:Any) extends DHTserverAPI


case class lookupForwardPut(key:String, value:String, keyhash:BigInt, hostNode:node) extends DHTserverAPI
case class lookupPredecessorFoundPut(key:String,value:String, keyhash:BigInt,nodeFound:node) extends DHTserverAPI
case class lookupGetSuccessorPut(key:String,value:String, keyhash:BigInt) extends DHTserverAPI
case class lookupSuccessorFoundPut(key:String,value:String,keyhash:BigInt,nodeFound:node) extends DHTserverAPI
case class lookupNodePut(key:String,value:String) extends DHTserverAPI


case class joinInitialize(hostNode:node) extends DHTserverAPI
case class joinRequest(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) extends DHTserverAPI
case class joinLookupForward(nodeName:String,nodeNameHash:BigInt,hostNode:node) extends DHTserverAPI
case class joinLookupPredecessorFound(nodeFound:node) extends DHTserverAPI
case class joinGetSuccessor() extends DHTserverAPI
case class joinLookupSuccessorFound(nodeFound:node) extends DHTserverAPI
case class joinMoveKeyValuesRequest(requestNode:node) extends DHTserverAPI
case class joinMoveKeyValuesResult(storePart:HashMap[String,Any]) extends DHTserverAPI


case class stabilizeStart() extends DHTserverAPI
case class stabilizeGetPredecessor() extends DHTserverAPI
case class stabilizePredecessorFound(nodeFound:node) extends DHTserverAPI
case class stabilizeNotify(mynode:node) extends DHTserverAPI
case class stabilizeHBStart() extends DHTserverAPI


case class fixFingerStart() extends DHTserverAPI
case class fixLookupForward(i:Int, fingerStart:BigInt, hostNode:node) extends DHTserverAPI
case class fixLookupPredecessorFound(i:Int, fingerStart:BigInt, nodeFound:node) extends DHTserverAPI
case class fixGetSuccessor(i:Int,fingerStart:BigInt) extends DHTserverAPI
case class fixLookupSuccessorFound(i:Int,figureStart:BigInt,nodeFound:node) extends DHTserverAPI
case class fixFingerHBStart() extends DHTserverAPI

case class startupFinger(finger:ArraySeq[node]) extends DHTserverAPI
case class startupPredecessor(myPredecessor:node) extends DHTserverAPI
case class poisonPill() extends DHTserverAPI









