package sample.remote.clients

import akka.actor.Props
import akka.actor._
import scala.collection.mutable._
import sample.remote.servers.{clientRequestTopology, clientNodeListRequest, node}
import scala.concurrent.duration._

class clientActor(path:String) extends Actor {

  private var DHTNodeList = Set[node]()

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      println("remote connection success")
      context.watch(actor)
      context.become(active(actor))
    case ActorIdentity(`path`, None) => println(s"Remote actor not available: $path")
    case ReceiveTimeout              => sendIdentifyRequest()
    case _                           => println("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case startup() =>
      println("send a node list request to remote master")
      actor ! clientRequestTopology()
      actor ! clientNodeListRequest()

    case DHTNodeListReturn(nodeList:Set[node]) =>
      DHTNodeList = nodeList
      println("I get a node list")

    case lookupNodeGetReturn(key:String,value:Any) =>
      println("The key-value pair is " + key + " - " + value)

//    case Terminated(`actor`) =>
//      println("Calculator terminated")
//      sendIdentifyRequest()
//      context.become(identifying)
    case ReceiveTimeout =>
    // ignore

    // request the topology of the DHT servers
    case DHTTopology(topology:HashMap[node,HashMap[String,Any]]) =>
      println("I received the topology of the DHTservers")
      for ((eachNode, localStore) <- topology) {
        println(eachNode.path)
        for ((k,v) <- localStore) {
          println(k + "  :  "  + v)
        }
      }

  }


}
