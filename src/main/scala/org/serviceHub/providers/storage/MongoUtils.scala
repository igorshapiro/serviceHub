package org.serviceHub.providers.storage

import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import org.serviceHub.domain.Message

/**
 * Created by igor on 3/20/15.
 */
object MongoUtils {
  val SERVICE_HUB_DB = "serviceHub"

  import org.serviceHub.domain.MessageJsonProtocol._
  import spray.json._

  def msgToDBObject(msg: Message) = JSON.parse(msg.toJson.prettyPrint).asInstanceOf[DBObject]
  def msgFromDBObject(obj: DBObject) = JSON.serialize(obj).parseJson.convertTo[Message]

  def collectionMessages(collection: String) = {

  }
}
