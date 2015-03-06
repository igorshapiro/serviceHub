package org.serviceHub.domain

import org.joda.time.DateTime
import spray.httpx.SprayJsonSupport
import spray.json._

object MessageJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport with NullOptions {
  implicit val messageSerializer = jsonFormat5(Message)
}

case class Message(
  messageType: String,
  content: JsValue = new JsObject(Map.empty),
  attemptsMade: Int = 0,
  maxAttempts: Int = 5,
  env: String = "default"
) {
  def attempted = this.copy(attemptsMade = attemptsMade + 1)
  def reachedMaxAttempts = this.attemptsMade >= this.maxAttempts
}

case class ScheduledMessage(message: Message, dueAt: DateTime, scheduledAt: DateTime)