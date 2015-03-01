package org.serviceHub.domain

import spray.json.{JsObject, JsValue}

/**
 * Created by igor on 2/28/15.
 */
case class Message(
  messageType: String,
  content: JsValue = new JsObject(Map.empty),
  attemptsMade: Int = 0,
  maxAttempts: Int = 5,
  env: String = "default"
)
