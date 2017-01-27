package global.helpers

import play.api.libs.json.{ JsObject, Json, Reads, _ }

object JsonHelper {
  def prefixKeys(json: JsObject, prefix: Symbol): JsObject = {
    val flatten = (__ \ prefix).read[JsObject].flatMap(
      _.fields.foldLeft((__ \ prefix).json.prune) {
        case (acc, (k, v)) => acc andThen __.json.update(
          Reads.of[JsObject].map(_ + (s"${prefix.name}.$k" -> v))
        )
      }
    )
    Json.obj(prefix.name -> json).transform(flatten).get
  }
}
