package org.serviceHub.providers.storage

import com.mongodb.util.JSON
import org.serviceHub.domain.{Service, Message}

abstract class StorageProvider(svc: Service) {
  def deadEntityName = s"${svc.name}_dead"
  def kill(msg: Message)
  def getDeadMessages: List[Message]
  def purgeEverything: Unit
}

class MongoStorageProvider(svc: Service) extends StorageProvider(svc) {
  import org.serviceHub.domain.MessageJsonProtocol._
  import spray.json._

  import com.mongodb.casbah.Imports._
  val client = MongoClient()
  val db = client("serviceHub")
  val deadCollection = db(deadEntityName)

  override def kill(msg: Message): Unit = {
    deadCollection.insert(msgToDBObject(msg))
  }

  override def purgeEverything: Unit = {
    deadCollection.dropCollection()
  }

  override def getDeadMessages: List[Message] = {
    val q = deadCollection.find().limit(100)
    val messages = for (
      obj <- q;
      msg = msgFromDBObject(obj)
    ) yield msg
    messages.toList
  }

    private def msgToDBObject(msg: Message) = JSON.parse(msg.toJson.prettyPrint).asInstanceOf[DBObject]
    private def msgFromDBObject(obj: DBObject) = JSON.serialize(obj).parseJson.convertTo[Message]
}
