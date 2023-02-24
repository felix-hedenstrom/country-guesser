package webapp.model

import sttp.client3.UriContext
import sttp.model.Uri

sealed abstract class Country(val path: Uri)

object Country {

  case object Sweden extends Country(uri"sweden")

  def parse(s: String): Option[Country] = if (s == "Sweden") Some(Country.Sweden) else None
}