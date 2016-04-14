package sample.remote.calculator

import akka.actor.{ActorRef,ActorPath}

/**
  * Created by Chaoren on 4/11/16.
  */

sealed trait DHTserverAPI
case class clientGet(key:String) extends DHTserverAPI
case class clientGetReturn(key:String,value:Any) extends DHTserverAPI

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


case class joinRequest(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) extends DHTserverAPI
case class joinLookupForward(nodeName:String,nodeNameHash:BigInt,hostNode:node) extends DHTserverAPI
case class joinLookupPredecessorFound(nodeFound:node) extends DHTserverAPI
case class joinGetSuccessor() extends DHTserverAPI
case class joinLookupSuccessorFound(nodeFound:node) extends DHTserverAPI


case class stabilizeGetPredecessor() extends DHTserverAPI
case class stabilizePredecessorFound(nodeFound:node) extends DHTserverAPI
case class stabilizeNotify(mynode:node) extends DHTserverAPI


case class fixLookupForward(i:Int, fingerStart:BigInt, hostNode:node) extends DHTserverAPI
case class fixLookupPredecessor(i:Int, fingerStart:BigInt, nodeFound:node) extends DHTserverAPI
case class fixGetSuccessor(i:Int,fingerStart:BigInt) extends DHTserverAPI
case class fixLookupSuccessorFound(i:Int,figureStart:BigInt,nodeFound:node) extends DHTserverAPI



sealed trait DHTobject
case class node(path:ActorPath,nameHash:BigInt,actorNode:ActorRef) extends DHTobject

