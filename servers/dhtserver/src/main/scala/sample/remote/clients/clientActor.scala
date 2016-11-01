package sample.remote.clients

import akka.actor.Props
import akka.actor._
import scala.collection.mutable._
import sample.remote.servers._
import scala.concurrent.duration._
import sample.remote.servers.utilities.{toHash,m}
import java.io._

class clientActor(path:String) extends Actor {

//  private var DHTNodeList = Set[node]()
  private var lookupNodeSet = Set[ActorRef]()
  val lookupNodeListSize = 5
//  private var masterActorRef:ActorRef = null

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      println("connect to DHT master")
      context.watch(actor)
      context.become(active(actor))
    case ActorIdentity(`path`, None) => println(s"Remote actor not available: $path")
    case ReceiveTimeout              => sendIdentifyRequest()
    case _                           => println("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case startup() =>
      println("request a node list from remote master")
      actor ! clientNodeListRequest()
      actor ! clientRequestTopology()
//      import context.dispatcher
//      context.system.scheduler.scheduleOnce(3 seconds) {
//        if (lookupNodeSet.size != 0)
//          println("I am trying to get a kv pair")
//          lookupNodeSet.toVector(0) ! clientGet("qwNQg9ouZdgNHcmj")
//      }
//      actor ! clientNodeCreation("hahahaNode3")
//      println("creating a new node")


    case DHTNodeRefReturn() =>
      println("I am getting a node pointer")
      lookupNodeSet += sender

//      println("I get a node list")
//      val hostNode = DHTNodeList.toVector(0)
//      hostNode.actorNode ! clientGet("HMiWh8u8td")

    case clientLookupNodeGetReturn(key:String,value:Any) =>
      value match {
        case Some(realvalue) => println("The key-value pair is " + key + " - " + realvalue)
        case None => println("The value for " + key +" cannot be found")
      }

//    case Terminated(`actor`) =>
//      println("Calculator terminated")
//      sendIdentifyRequest()
//      context.become(identifying)
    case ReceiveTimeout =>
    // ignore

    // request the topology of the DHT servers
    case DHTTopology(topology:HashMap[node,HashMap[String,Any]]) =>
      println("I received the topology of the DHTservers")
      prepareForTopologyFigure(topology)
//      for ((eachNode, localStore) <- topology) {
//        println(eachNode.path)
//        for ((k,v) <- localStore) {
//          println(k + "  :  "  + v)
//        }
//      }

  }

  def prepareForTopologyFigure(topology:HashMap[node,HashMap[String,Any]]): Unit = {
//    val sortedNodeHash = topology.keySet.toVector.sortBy[BigInt](_.nameHash)
//    val smallestNodeHash = sortedNodeHash(0).nameHash
    val output = new PrintWriter(new File("data/topology_before"))
    for ((eachNode, localStore)  <- topology) {
      for ((k, v) <- localStore) {
        val keyHash = toHash(k)
        if (keyHash > eachNode.nameHash)
          output.write(eachNode.path.name + "\t" + eachNode.nameHash.toString() + "\t" + (keyHash - BigInt(2).pow(m)).toString() + "\n")
        else
          output.write(eachNode.path.name + "\t" + eachNode.nameHash.toString() + "\t" + keyHash.toString() + "\n")
      }
    }
    output.close()
  }



}
