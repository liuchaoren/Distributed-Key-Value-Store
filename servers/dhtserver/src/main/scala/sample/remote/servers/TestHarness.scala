package sample.remote.servers

import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.util.Timeout

/**
  * Created by Chaoren on 4/14/16.
  */
object TestHarness {
  val initialNumOfNode = 10
  val initialNumOfKVs = 100
  val system = ActorSystem("DHTservers", ConfigFactory.load("DHTservers"))
  implicit val timeout = Timeout(60 seconds)

  val master =system.actorOf(Props(classOf[MasterActor], system, initialNumOfNode, initialNumOfKVs), "MasterActor")

  def main(args: Array[String]): Unit = run()
  def run():Unit = {
    master ! startup()
  }

}
