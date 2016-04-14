package sample.remote.calculator

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props, Actor, ActorSelection, ActorSystem}
import akka.util.Timeout

import com.roundeights.hasher.Implicits._
import scala.collection.mutable
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Future,Await}



class DHTActor extends Actor {
  val m = 160
  private val store = new mutable.HashMap[String, Any]
  private val finger = new mutable.ArraySeq[node](m)
  private var predecessor: node = null

  val nodeName = self.path.name
  val nodeHash = toHash(nodeName)
  val mynode = node(self.path,nodeHash,self)

  implicit val timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  // if key-value is not in the local storage, put the client and the key (client asks for) to waitList
  private val waitList = new mutable.HashMap[ActorRef,Set[String]]

  def receive = {

    // kv store operations
    //    case get(key) =>
    //      sender ! store.get(key)
    //    case put(key,value) =>
    //      sender ! store.put(key,value)

    //    case salutation(words:String) =>
    //      println("I received salutation from a client!")
    //      println("MD5" + "some string".sha1.hex)

    // for clients
    case clientGet(key:String) =>
      val keyHash = toHash(key)
      if (rangeTeller(predecessor.nameHash,mynode.nameHash,keyHash))
        sender ! store.get(key)
      else {
        putWaitList(sender, key)
        val firstStation = closest_preceding_figure(keyHash)
        firstStation.actorNode ! lookupForward(key,keyHash,mynode)
      }

    case lookupForward(key:String, keyHash:BigInt, hostNode:node) =>
      if (rangeTeller(mynode.nameHash,finger(0).nameHash,keyHash))
        hostNode.actorNode ! lookupPredecessorFound(key,keyHash,mynode)
      else {
        val nextStation = closest_preceding_figure(keyHash)
        nextStation.actorNode ! lookupForward(key,keyHash,hostNode)
      }

    case lookupPredecessorFound(key:String,keyHash:BigInt,predecessorNode:node) =>
      predecessorNode.actorNode ! lookupGetSuccessor(key,keyHash)

    case lookupGetSuccessor(key:String,keyHash:BigInt) =>
      sender ! lookupSuccessorFound(key,keyHash,finger(0))

    case lookupSuccessorFound(key:String,keyHash:BigInt,successorNode:node) =>
      successorNode.actorNode ! lookupNodeGet(key)

    case lookupNodeGet(key) =>
      val value = store.get(key)
      sender ! lookupNodeGetReturn(key,value)

    case lookupNodeGetReturn(key:String,value:Any) =>
      send2Clients(lookupNodeGetReturn(key,value))



  // join
    case joinRequest(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) =>
      if (rangeTeller(predecessor.nameHash, nodeHash, requestNodeHash))
        requestNode.actorNode ! joinLookupSuccessorFound(mynode)
      else {
        val firstStation = closest_preceding_figure(requestNodeHash)
        firstStation.actorNode ! joinLookupForward(requestNodeName,requestNodeHash,requestNode)
      }

    case joinLookupForward(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) =>
      if (rangeTeller(mynode.nameHash, finger(0).nameHash, requestNodeHash))
        requestNode.actorNode ! joinLookupPredecessorFound(mynode)
      else {
        val nextStation = closest_preceding_figure(requestNodeHash)
        nextStation.actorNode ! joinLookupForward(requestNodeName,requestNodeHash,requestNode)
      }

    case joinLookupPredecessorFound(predecessorNode:node) =>
      predecessor.actorNode ! joinGetSuccessor()

    case joinGetSuccessor()  =>
      sender ! joinLookupSuccessorFound(finger(0))

    case joinLookupSuccessorFound(successorNode:node) =>
      finger(0) = successorNode



    // stabilization
    case stabilizeGetPredecessor() =>
      sender ! stabilizePredecessorFound(predecessor)

    case stabilizePredecessorFound(stabilizePredecessor:node) =>
      if (rangeTeller(nodeHash, finger(0).nameHash, stabilizePredecessor.nameHash))
        finger(0) = stabilizePredecessor
      finger(0).actorNode ! stabilizeNotify(mynode)

    case stabilizeNotify(notifyNode:node) =>
      if (predecessor == null || rangeTeller(predecessor.nameHash, nodeHash, notifyNode.nameHash))
        predecessor = notifyNode



    // fix fingers
    case

  }


  //  def find_successor(id:String): node = {
  //    val nprime = find_predecessor(id)
  //    return nprime.finger(0)
  //  }

  def closest_preceding_figure(id:BigInt): node = {
    for (i <- 0 to m - 1) {
      if (finger(i).nameHash > nodeHash && finger(i).nameHash < id)
        return finger(i)
    }
    return mynode
  }

//  def get(key: String): Any = {
//    return keyvalue(key, store.get(key))
//  }

//  def getActorRef(oneNode:node): Future[ActorRef] = {
//    return context.actorSelection(oneNode.path).resolveOne()
//  }

  def rangeTeller(start:BigInt, end:BigInt, point:BigInt): Boolean = {
    if (start <= end) {
      if (point >= start && point < end)
        return true
      else
        return false
    }
    else {
      if (point >= start || point < end)
        return true
      else
        return false
    }
  }

  def toHash(s:String):BigInt = {
    return BigInt(s.sha1.bytes)
  }

  def putWaitList(client:ActorRef, key:String):Unit = {
    if (waitList.contains(client))
      waitList.get(client) += key
    else
      waitList.put(client, Set(key))
  }

  def send2Clients(lookupResult:lookupNodeGetReturn):Unit = {
    for ((client,keysWaiting) <- waitList) {
      if (keysWaiting.isEmpty)
        waitList.remove(client)

      if (keysWaiting.contains(lookupResult.key)) {
        client ! lookupResult
        keysWaiting -= lookupResult.key
      }
    }
  }

}
