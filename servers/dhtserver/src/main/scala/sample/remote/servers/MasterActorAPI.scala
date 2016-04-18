package sample.remote.servers

/**
  * Created by Chaoren on 4/15/16.
  */

sealed trait MasterAPI

// clients
case class clientNodeCreation(nodeName:String) extends MasterAPI
case class clientRandomNodeKill(n:Int) extends MasterAPI
case class clientNodeListRequest() extends MasterAPI
case class clientNodeCreationSuccessorInitialized() extends MasterAPI

case class clientRequestTopology() extends MasterAPI

// test main
case class startup() extends MasterAPI
case class startupFingerReceived(receivedNode:node) extends MasterAPI
case class startupPredecessorReceived(receivedNode:node) extends MasterAPI
case class startupHBReceived(receivedNode:node) extends MasterAPI




