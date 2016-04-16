package sample.remote.calculator
/**
  * Created by Chaoren on 4/14/16.
  */


import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Props, Actor, ActorSelection, ActorSystem}
import akka.util.Timeout

import com.roundeights.hasher.Implicits._
import scala.collection.mutable
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.{Future,Await}
import scala.collection.mutable.{ArrayBuffer, Set}
import utilities._
import scala.util.Random
import util.control.Breaks._

class MasterActor(system:ActorSystem, numOfNodes:Int, numOfKVs:Int) extends Actor {

  private val DHTNodeList = Set[node]()
  private var counterFingerReceived = 0

  def receive = {


    case startup() =>
      for (i <- 1 to numOfNodes) {
        val nodeName = "DHTnode" + i
        val nodeNameHash = toHash(nodeName)
        val nodeActorRef = system.actorOf(Props(classOf[DHTActor]), nodeName)
        val thisNode = node(nodeActorRef.path,nodeNameHash,nodeActorRef)
        DHTNodeList += thisNode
      }

      val fingerTables = fingerTableCreation(DHTNodeList)
      for ((onenode, fingerAndPredecessor) <- fingerTables)
        onenode.actorNode ! startupFinger(fingerAndPredecessor)


    case starupFingerReceived(receivedNode:node) =>
      counterFingerReceived += 1
      if (counterFingerReceived == numOfNodes) {
        for (eachNode <- DHTNodeList) {
          eachNode.actorNode ! stabilizeHBStart()
          eachNode.actorNode ! fixFingerHBStart()

        }
        populateNodes(numOfKVs,DHTNodeList)
      }


    case clientNodeCreation(nodeName:String) =>
      val newNodeActorRef = system.actorOf(Props(classOf[DHTActor]),nodeName)
      val nodeNameHash = toHash(nodeName)
      val newNode = node(newNodeActorRef.path,nodeNameHash,newNodeActorRef)
      val rnd = new Random
      val hostNode = DHTNodeList.toVector(rnd.nextInt(DHTNodeList.size))

      DHTNodeList += newNode
      newNodeActorRef ! joinInitialize(hostNode)


    case clientRandomNodeKill(n:Int) =>
      val randomNode = new Random
      val nodeSList = DHTNodeList.toVector
      val killIndex = Set[Int]()

      breakable {
        while (true) {
          val nextKill = randomNode.nextInt(nodeSList.size)
          if (! killIndex.contains(nextKill))
            killIndex += nextKill
          if (killIndex.size == n)
            break
        }
      }

      for (eachKillNodeIndex <- killIndex) {
        nodeSList(eachKillNodeIndex).actorNode ! poisonPill()

      }

  }


  def randomString(length: Int) = scala.util.Random.alphanumeric.take(length).mkString

  def populateNodes(numOfKVs:Int, nodesList:Set[node]): Unit = {
    val randomStringLen = new Random
    val randomNode = new Random
    for (i <- 1 to numOfKVs) {
      val keyLen = randomStringLen.nextInt(25) + 8
      val valueLen = randomStringLen.nextInt(25) + 8
      val key = randomString(keyLen)
      val value = randomString(valueLen)
      val hostNode = nodesList.toVector(randomNode.nextInt(nodesList.size))
      hostNode.actorNode ! clientPut(key, value)
    }

  }

  def fingerTableCreation(nodeList:Set[node]): mutable.HashMap[node, Tuple2[mutable.ArraySeq[node], node]] = {
    val nodeListOrdered = nodeList.toVector.sortBy[BigInt](_.nameHash)
    val nodeFingerMap = new mutable.HashMap[node, Tuple2[mutable.ArraySeq[node],node]]
    val m = 160
    for (i <- 0 to nodeListOrdered.size - 1) {
      val targetHash = nodeListOrdered(i).nameHash
      val targetFinger = new mutable.ArraySeq[node](m)
      for (fingerindex <- 0 to m-1) {
        val fingerStart = (BigInt(2).pow(fingerindex) + targetHash).mod(BigInt(2).pow(m))
        val oneFinger = successorNode(nodeListOrdered,fingerStart)
        targetFinger(fingerindex) = oneFinger
      }
      if (i == 0)
        nodeFingerMap.put(nodeListOrdered(i), Tuple2(targetFinger, nodeListOrdered(nodeListOrdered.size-1)))
      else
        nodeFingerMap.put(nodeListOrdered(i), Tuple2(targetFinger, nodeListOrdered(i-1)))
    }
    return nodeFingerMap
  }

  def successorNode(nodeListOrdered:Vector[node], fingerStart:BigInt): node = {
    for (i <- 0 to nodeListOrdered.size-2) {
      if (rangeTeller(nodeListOrdered(i).nameHash, nodeListOrdered(i+1).nameHash, fingerStart))
        return nodeListOrdered(i+1)
    }
    return nodeListOrdered(nodeListOrdered.size-1)
  }

}


