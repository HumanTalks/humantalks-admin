package global.helpers

import java.security.MessageDigest

object StringHelper {
  def toMd5(str: String): String = MessageDigest.getInstance("MD5").digest(str.getBytes).map("%02x".format(_)).mkString
}
