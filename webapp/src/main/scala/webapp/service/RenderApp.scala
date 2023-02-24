package webapp.service

import colibri.{BehaviorSubject, Subject}
import colibri.ext.zio._
import org.scalajs.dom.KeyCode
import outwatch.VNode
import outwatch.dsl._
import webapp.http.ObjectiveListClient
import webapp.model.Country
import zio.{Task, UIO, ZIO, ZLayer}
import org.scalajs.dom.console
import outwatch.helpers.OutwatchTracing

trait RenderApp {
  def app: Task[VNode]
}

// https: //outwatch.github.io/docs/readme.html
object RenderApp {
  val live: ZLayer[ObjectiveListClient, Nothing, RenderApp] = ZLayer.fromZIO{
    for {
      olc <- ZIO.service[ObjectiveListClient]
    } yield new RenderApp {
      override def app: Task[VNode] = {
        for {
          country <- correctCountry
          _ <- ZIO.attempt{
            OutwatchTracing.error.unsafeForeach { throwable =>
              console.log(s"Exception while patching an Outwatch compontent: ${throwable.getMessage}")
            }
          }

        finishedApp <- ZIO.attempt{
        div(
          h1("Country Guesser"),
          similarityGuess(country)
        )
      }
        } yield finishedApp
      }



      val onEnter = onKeyDown.filter(_.keyCode == KeyCode.Enter)
      def correctCountry: ZIO[Any, Nothing, BehaviorSubject[Country]] = olc.generateCountry.map(Subject.behavior)

      def similarityGuess(correctCountry: BehaviorSubject[Country]): VNode = {
        def check(maybeGuess: Option[Country], correct: Country): UIO[String] = maybeGuess match {
          case Some(guess) => olc.checkSimilarity(correct, guess).fold(
            e => s"Got an error $e",
            score => s"Score: $score"
          )
          case None => ZIO.succeed(s"Not a valid guess $correct $maybeGuess")
        }

        // https://outwatch.github.io/docs/readme.html#example-input-field
        val text = Subject.behavior("")
        val submitted = Subject.behavior[Option[Country]](None)
        div(
          input(
            tpe := "text",
            value <-- text,
            onInput.value --> text,
            onEnter(text).map(Country.parse) --> submitted
          ),
          div("submitted: ", submitted.map(_.toString())),
          div("distance: ", submitted.combineLatestMap(correctCountry)(check)),
          div("correct: ", correctCountry.map(_.toString)),
          div("text: ", text),
          div("length: ", text.map(_.length)),
        )
      }
    }
  }
}
