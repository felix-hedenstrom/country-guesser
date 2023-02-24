package webapp.http

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import zio.{Task, ULayer, ZLayer}



object BaseClient {

  val live: ULayer[BaseClient] = ZLayer.succeed {
      sttp.client3.impl.zio.FetchZioBackend()
  }

  type BaseClient = SttpBackend[Task, ZioStreams with WebSockets]
}
