package sample.remote.servers
/**
  * MasterActor initializes the DHT server and receive request to start new node or kill existing node
  */


import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props, Actor, ActorSelection, ActorSystem}
import akka.util.Timeout
import akka.pattern.ask

import com.roundeights.hasher.Implicits._
import sample.remote.clients.{DHTNodeListReturn,DHTTopology}
import scala.collection.mutable.{HashMap,Set,ArraySeq}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Future,Await}
import utilities._
import scala.util.Random
import util.control.Breaks._
import akka.util.Timeout

class MasterActor(system:ActorSystem, numOfNodes:Int, numOfKVs:Int) extends Actor {

  // keep a set of DHT nodes in DHTNodeList
  private val DHTNodeList = Set[node]()
  private var counterFingerReceived = 0
  private var counterPredecessorReceived = 0
  private var notHBYet = true
  private val lookupNodeListSize = 5
  val randomStringLen = new Random
  val randomNode = new Random
  val rnd = new Random
  val randomNodeKill = new Random

  implicit val timeout = Timeout(5 seconds)

  val askLocalKVsTimeout = 2 seconds


  def receive = {

    case startup() =>
      println("starting up the DHT server!")
      for (i <- 1 to numOfNodes) {
        val nodeName = "DHTnode" + i
        val nodeNameHash = toHash(nodeName)
        val nodeActorRef = system.actorOf(Props(classOf[DHTActor]), nodeName)
        val thisNode = node(nodeActorRef.path,nodeNameHash,nodeActorRef)
        DHTNodeList += thisNode
      }
      println("finished in creating server actors")

      val fingerTables = fingerTableCreation(DHTNodeList)
      val predecessorsList = predecessorCreation(DHTNodeList)
      // pass finger tables and predecessors to nodes
      for ((onenode, finger) <- fingerTables)
        onenode.actorNode ! startupFinger(finger)
      for ((onenode, onePredecessor)<-predecessorsList)
        onenode.actorNode ! startupPredecessor(onePredecessor)


    // after all nodes get finger tables and predecessors, start heart beats and populate the nodes
    case startupFingerReceived(receivedNode:node) =>
      counterFingerReceived += 1
      println(counterFingerReceived + " received ack for one finger table passing")
      if (counterFingerReceived == numOfNodes && counterPredecessorReceived == numOfNodes && notHBYet) {
        for (eachNode <- DHTNodeList) {
          eachNode.actorNode ! stabilizeHBStart()
          eachNode.actorNode ! fixFingerHBStart()
        }
        notHBYet = false
        println("start to populate servers")
        populateNodes(numOfKVs,DHTNodeList)
      }

    case startupPredecessorReceived(receivedNode:node) =>
      counterPredecessorReceived += 1
      println(counterPredecessorReceived + " received ack for one predecessor passing")
      if (counterFingerReceived == numOfNodes && counterPredecessorReceived == numOfNodes && notHBYet) {
//        println("start the heart beats")
        for (eachNode <- DHTNodeList) {
          eachNode.actorNode ! stabilizeHBStart()
          eachNode.actorNode ! fixFingerHBStart()
        }
        notHBYet = false
        println("start to populate servers")
        populateNodes(numOfKVs,DHTNodeList)
      }

    // handle request of creating an node
    case clientNodeCreation(nodeName:String) =>
      val newNodeActorRef = system.actorOf(Props(classOf[DHTActor]),nodeName)
      val nodeNameHash = toHash(nodeName)
      val newNode = node(newNodeActorRef.path,nodeNameHash,newNodeActorRef)
      val hostNode = DHTNodeList.toVector(rnd.nextInt(DHTNodeList.size))

      DHTNodeList += newNode
      newNodeActorRef ! joinInitialize(hostNode)

    // handle request of kill n nodes randomly
    case clientRandomNodeKill(n:Int) =>
      val nodeSList = DHTNodeList.toVector
      val killIndex = Set[Int]()

      breakable {
        while (true) {
          val nextKill = randomNodeKill.nextInt(nodeSList.size)
          if (! killIndex.contains(nextKill))
            killIndex += nextKill
          if (killIndex.size == n)
            break
        }
      }

      for (eachKillNodeIndex <- killIndex) {
        nodeSList(eachKillNodeIndex).actorNode ! poisonPill()

      }


    // client requests a list of nodes
    case clientNodeListRequest() =>
      println("some client requires a list of node")
      val nodeSList = DHTNodeList.toVector
      val returnNodeIndex = Set[Int]()
      val returnNodes = Set[node]()

      breakable {
        while (true) {
          val nextNode = randomNode.nextInt(nodeSList.size)
          if (! returnNodeIndex.contains(nextNode))
            returnNodeIndex += nextNode
          if (returnNodeIndex.size==lookupNodeListSize)
            break
        }
      }
      for (eachNodeIndex <- returnNodeIndex)
        returnNodes += nodeSList(eachNodeIndex)

      sender ! DHTNodeListReturn(returnNodes)


    case clientRequestTopology() =>
      val topology = new HashMap[node, HashMap[String,Any]]
      for (eachNode <- DHTNodeList) {
        val future = ask(eachNode.actorNode, requestLocalKVs()).mapTo[HashMap[String,Any]]
        val oneLocalKVs = Await.result(future, askLocalKVsTimeout)
        topology.put(eachNode,oneLocalKVs)
      }

      sender ! DHTTopology(topology)

  }

  // generate an random string of random length
  def randomString(length: Int) = scala.util.Random.alphanumeric.take(length).mkString

  // populate nodes with random keys and random values
  def populateNodes(numOfKVs:Int, nodesList:Set[node]): Unit = {
    for (i <- 1 to numOfKVs) {
      val keyLen = randomStringLen.nextInt(25) + 8
      val valueLen = randomStringLen.nextInt(25) + 8
      val key = randomString(keyLen)
      val value = randomString(valueLen)
      val hostNode = nodesList.toVector(randomNode.nextInt(nodesList.size))
      hostNode.actorNode ! clientPut(key, value)
    }

  }
  // return the finger tables for a set of nodes
  def fingerTableCreation(nodeList:Set[node]):HashMap[node, ArraySeq[node]] = {
    val nodeListOrdered = nodeList.toVector.sortBy[BigInt](_.nameHash)
    val nodeFingerMap = new HashMap[node, ArraySeq[node]]
    for (i <- 0 to nodeListOrdered.size - 1) {
      val targetHash = nodeListOrdered(i).nameHash
      val targetFinger = new ArraySeq[node](m)
      for (fingerindex <- 0 to m-1) {
        val fingerStart = (BigInt(2).pow(fingerindex) + targetHash).mod(BigInt(2).pow(m))
        val oneFinger = successorNode(nodeListOrdered,fingerStart)
        targetFinger(fingerindex) = oneFinger
      }
      nodeFingerMap.put(nodeListOrdered(i), targetFinger)
    }
    return nodeFingerMap
  }

  // return the predecessors for a set of nodes
  def predecessorCreation(nodeList:Set[node]):HashMap[node,node] = {
    val nodeListOrdered = nodeList.toVector.sortBy[BigInt](_.nameHash)
    val nodePredecessorMap = new HashMap[node,node]
    for (i <- 1 to nodeListOrdered.size - 1) {
      nodePredecessorMap.put(nodeListOrdered(i), nodeListOrdered(i-1))
    }
    nodePredecessorMap.put(nodeListOrdered(0),nodeListOrdered(nodeListOrdered.size-1))
    return nodePredecessorMap
  }


  def successorNode(nodeListOrdered:Vector[node], fingerStart:BigInt): node = {
    for (i <- 0 to nodeListOrdered.size-2) {
      if (rangeTellerEqualRight(nodeListOrdered(i).nameHash, nodeListOrdered(i+1).nameHash, fingerStart))
        return nodeListOrdered(i+1)
    }
    return nodeListOrdered(0)
  }

}


