package webapp

import outwatch._
import webapp.http.{BaseClient, ObjectiveListClient}
import webapp.service.RenderApp
import zio.interop.catz.asyncInstance
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

// Outwatch documentation:
// https://outwatch.github.io/docs/readme.html

object Main extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    for {
      renderer <- ZIO.service[RenderApp]
      app <- renderer.app
      _ <- Outwatch.renderInto[Task]("#app", app)
    } yield ()
  }.provide(layers)

  val layers = ZLayer.make[RenderApp](
    BaseClient.live,
    ObjectiveListClient.live,
    RenderApp.live
  )
}
