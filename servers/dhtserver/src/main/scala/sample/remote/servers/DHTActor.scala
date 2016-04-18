package sample.remote.servers

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.util.Timeout

import com.roundeights.hasher.Implicits._
import scala.collection.mutable.{ArraySeq,Set,HashMap}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Future,Await}
import utilities._
import java.util.concurrent.TimeUnit

import scala.util.Random
case class node(path:ActorPath,nameHash:BigInt,actorNode:ActorRef)

class DHTActor extends Actor {
  private val store = new HashMap[String, Any]
  private var finger = new ArraySeq[node](m)
  private var predecessor:node = null

  val nodeName = self.path.name
  val nodeHash = toHash(nodeName)
  val mynode = node(self.path,nodeHash,self)

  implicit val timeout = Timeout(FiniteDuration(5, TimeUnit.SECONDS))
  val stabilizeHBInterval = FiniteDuration(10, TimeUnit.SECONDS)
  val fixFingerHBInterval = FiniteDuration(10, TimeUnit.SECONDS)
  val randomFingerIndex = new Random

  // if key-value is not in the local storage, put the client and the key (client asks for) to waitList
  private val waitList = new HashMap[ActorRef,Set[String]]

  import context.dispatcher

//
//  override def preStart(): Unit = {
//    val firstJoinTask = context.system.scheduler.scheduleOnce(0, self, joinRequest())
//
//  }


  def receive = {

    // client get *************************************************************
    case clientGet(key:String) =>
      val keyHash = toHash(key)
      if (rangeTellerEqualRight(predecessor.nameHash,mynode.nameHash,keyHash))
        sender ! store.get(key)
      else {
        putWaitList(sender, key)
        val firstStation = closest_preceding_finger(keyHash)
        firstStation.actorNode ! lookupForward(key,keyHash,mynode)
      }

    case lookupForward(key:String, keyHash:BigInt, hostNode:node) =>
      if (rangeTellerEqualRight(mynode.nameHash,finger(0).nameHash,keyHash))
        hostNode.actorNode ! lookupPredecessorFound(key,keyHash,mynode)
      else {
        val nextStation = closest_preceding_finger(keyHash)
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

    // client put ****************************************************************
    case clientPut(key:String,value:String) =>
      val keyHash = toHash(key)
      if (rangeTellerEqualRight(predecessor.nameHash,mynode.nameHash,keyHash)) {
        println("one kv pair populate me without message forwarding")
//        println("hash of " + key + "is" + keyHash)
        store.put(key,value)
      }
      else {
        val firstStation = closest_preceding_finger(keyHash)
        firstStation.actorNode ! lookupForwardPut(key,value, keyHash,mynode)
//        println("I am forwarding a message")
      }

    case lookupForwardPut(key:String, value:String, keyHash:BigInt, hostNode:node) =>
      if (rangeTellerEqualRight(mynode.nameHash,finger(0).nameHash,keyHash))
        hostNode.actorNode ! lookupPredecessorFoundPut(key,value,keyHash,mynode)
      else {
        val nextStation = closest_preceding_finger(keyHash)
        nextStation.actorNode ! lookupForwardPut(key,value,keyHash,hostNode)
//        println("I am forwarding a messag")
      }

    case lookupPredecessorFoundPut(key:String,value:String,keyHash:BigInt,predecessorNode:node) =>
      predecessorNode.actorNode ! lookupGetSuccessorPut(key,value,keyHash)

    case lookupGetSuccessorPut(key:String,value:String,keyHash:BigInt) =>
      sender ! lookupSuccessorFoundPut(key,value,keyHash,finger(0))

    case lookupSuccessorFoundPut(key:String,value:String,keyHash:BigInt,successorNode:node) =>
      successorNode.actorNode ! lookupNodePut(key,value)

    case lookupNodePut(key:String,value:String) =>
      println("one kv pair populate me with message forwarding")
      store.put(key,value)


  // join ************************************************************************
    case joinInitialize(hostNode:node) =>
      hostNode.actorNode ! joinRequest(nodeName, nodeHash, mynode)


    case joinRequest(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) =>
      if (rangeTellerEqualRight(predecessor.nameHash, nodeHash, requestNodeHash))
        requestNode.actorNode ! joinLookupSuccessorFound(mynode)
      else {
        val firstStation = closest_preceding_finger(requestNodeHash)
        firstStation.actorNode ! joinLookupForward(requestNodeName,requestNodeHash,requestNode)
      }

    case joinLookupForward(requestNodeName:String,requestNodeHash:BigInt,requestNode:node) =>
      if (rangeTellerEqualRight(mynode.nameHash, finger(0).nameHash, requestNodeHash))
        requestNode.actorNode ! joinLookupPredecessorFound(mynode)
      else {
        val nextStation = closest_preceding_finger(requestNodeHash)
        nextStation.actorNode ! joinLookupForward(requestNodeName,requestNodeHash,requestNode)
      }

    case joinLookupPredecessorFound(predecessorNode:node) =>
      predecessor.actorNode ! joinGetSuccessor()

    case joinGetSuccessor()  =>
      sender ! joinLookupSuccessorFound(finger(0))

    case joinLookupSuccessorFound(successorNode:node) =>
      finger(0) = successorNode
      finger(0).actorNode ! joinMoveKeyValuesRequest(mynode)

    case joinMoveKeyValuesRequest(requestNode:node) =>
      sender ! joinMoveKeyValuesResult(storePartition(requestNode))

    case joinMoveKeyValuesResult(partOfStore:HashMap[String,Any]) =>
      for ((k,v) <- partOfStore)
        store.put(k,v)


    // stabilization *************************************************************************
    case stabilizeStart() =>
      println("stabilize heart beating")
      if (finger(0) != null)
        finger(0).actorNode ! stabilizeGetPredecessor()

    case stabilizeGetPredecessor() =>
      if (predecessor != null)
        sender ! stabilizePredecessorFound(predecessor)

    case stabilizePredecessorFound(stabilizePredecessor:node) =>
      if (rangeTellerEqualRight(nodeHash, finger(0).nameHash, stabilizePredecessor.nameHash))
        finger(0) = stabilizePredecessor
      finger(0).actorNode ! stabilizeNotify(mynode)

    case stabilizeNotify(notifyNode:node) =>
      if (predecessor == null || rangeTellerEqualRight(predecessor.nameHash, nodeHash, notifyNode.nameHash))
        predecessor = notifyNode

    case stabilizeHBStart() =>
      println("stabilize heart beat starts")
      context.system.scheduler.schedule(0 second, stabilizeHBInterval,self,stabilizeStart())


    // fix fingers ***************************************************************************
    case fixFingerStart() =>
      println("fix finger heart beating")
      val fingerIndex = randomFingerIndex.nextInt(m)
      if (fingerIndex!=0) {
        val fingerStart = (BigInt(2).pow(fingerIndex) + nodeHash).mod(BigInt(2).pow(m))
        if (rangeTellerEqualRight(nodeHash, finger(0).nameHash, fingerStart))
          finger(fingerIndex) = finger(0)
        else {
          val nextStation = closest_preceding_finger(fingerStart)
          nextStation.actorNode ! fixLookupForward(fingerIndex,fingerStart,mynode)
        }
      }


    case fixLookupForward(i:Int,id:BigInt,hostNode:node) =>
      if (rangeTellerEqualRight(mynode.nameHash,finger(0).nameHash,id))
        hostNode.actorNode ! fixLookupPredecessorFound(i,id,mynode)
      else {
        val nextStation = closest_preceding_finger(id)
        nextStation.actorNode ! fixLookupForward(i,id,hostNode)
      }

    case fixLookupPredecessorFound(i:Int,id:BigInt,predecessorNode:node) =>
      predecessorNode.actorNode ! fixGetSuccessor(i,id)

    case fixGetSuccessor(i:Int,id:BigInt) =>
      sender ! fixLookupSuccessorFound(i,id,finger(0))

    case fixLookupSuccessorFound(i:Int,id:BigInt,successorNode:node) =>
      finger(i) = successorNode

    case fixFingerHBStart()  =>
      println("fix finger table heart beats starts")
      context.system.scheduler.schedule(0 second, fixFingerHBInterval, self, fixFingerStart())


   // initialize
    case startupFinger(inputFinger:ArraySeq[node]) =>
      println("received initial finger table")
      finger=inputFinger
      sender ! startupFingerReceived(mynode)

    case startupPredecessor(inputPredecessor:node) =>
      println("received initial predecessor")
      predecessor = inputPredecessor
      sender ! startupPredecessorReceived(mynode)


   // kill
    case poisonPill() =>
      context.stop(self)
  }



  //  def find_successor(id:String): node = {
  //    val nprime = find_predecessor(id)
  //    return nprime.finger(0)
  //  }
  // function definition ********************************************************************
  def closest_preceding_finger(id:BigInt): node = {
    for (i <- m-1 to 0 by -1) {
      if (finger(i) != null && rangeTellerEqualRight(nodeHash,id,finger(i).nameHash))
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

  // if key-value is not found locally, put the request into a waiting list
  def putWaitList(client:ActorRef, key:String):Unit = {
    waitList.get(client) match {
      case Some(waitingKeys) => waitingKeys += key
      case None => waitList.put(client,Set(key))
    }
  }

  // when receiving an return from lookup, iterate the waiting list and send results to the right clients
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

  // when new node joins, redistribute key-values
  def storePartition(requestNode:node):HashMap[String,Any] = {
    val storeResult = new HashMap[String,Any]()
    for ((key,value) <- store) {
      if (rangeTellerEqualRight(predecessor.nameHash, requestNode.nameHash, toHash(key)))
        storeResult.put(key,value)
        store.remove(key)
    }
    return storeResult

  }

}
