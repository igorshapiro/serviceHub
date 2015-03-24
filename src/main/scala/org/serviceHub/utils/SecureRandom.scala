package org.serviceHub.utils

import java.security.SecureRandom

object SecureRandom {
  val secureRandom = new java.security.SecureRandom()
  def newId(prefix: String = "", bytes: Int = 6) = {
    val randomBytes = new Array[Byte](bytes)
    val rand = new SecureRandom()
    rand.nextBytes(randomBytes)
    val randString = randomBytes.map("%02x" format _).mkString
    if (prefix != null && !prefix.isEmpty) s"${prefix}_$randString"
    else randString
  }
}
