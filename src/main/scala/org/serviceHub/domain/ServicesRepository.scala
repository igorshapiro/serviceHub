package org.serviceHub.domain

import java.io.File

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

import scala.io.Source

case class NoAuthorityForMessageException(messageType: String, publishingServices: Seq[Service])
  extends Exception(s"No authority for message $messageType: " +
    s"it's published by more than one service: ${publishingServices.map(_.name).mkString(", ")}")

class ServicesRepository(val services: Service*) {
  detectNonAuthoritativeMessages

  def this(workingDir: String) = this(ServicesRepository.loadServicesFromFile(new File(workingDir, "services.json")):_*)

  def getSubscribersFor(msg: Message) = services.filter(_.isSubscriberOf(msg))
  def getPublisherOf(msg: Message) = services.find(_.isPublisherOf(msg))
  def stopAllServices = {}

  def detectNonAuthoritativeMessages = {
    val messagePublishers = services.foldLeft(Map[String, List[Service]]())((acc, svc) =>
      svc.publishes.foldLeft(acc)((acc, msg) =>
        if (!acc.contains(msg))
          acc.updated(msg, List(svc))
        else
          acc.updated(msg, acc(msg) ++ List(svc))
      )
    )
    messagePublishers.find(_._2.length > 1) match {
      case Some((msg, services)) => throw new NoAuthorityForMessageException(msg, services)
      case None =>
    }
  }
}

object ServicesRepository {
  def loadServicesFromFile(f: File): List[Service] = {
    implicit val formats = DefaultFormats
    val jsonString = Source.fromFile(f).mkString
    val json = JsonMethods.parse(jsonString)
    ((json \\ "services") transformField(Service.jsonTransformer)).extract[List[Service]]
  }
}