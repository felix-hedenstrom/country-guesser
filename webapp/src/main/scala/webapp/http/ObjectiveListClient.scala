package webapp.http

import sttp.client3.{UriContext, basicRequest}
import sttp.model.Uri
import webapp.http.BaseClient.BaseClient
import webapp.http.ObjectiveListClient.ObjectiveClientError
import webapp.model.{Country, Score}
import zio.{IO, UIO, ZIO, ZLayer}

trait ObjectiveListClient {

  def checkSimilarity(country1: Country, country2: Country): IO[ObjectiveClientError, Score]

  def generateCountry: UIO[Country]
}

object ObjectiveListClient {


  val live: ZLayer[BaseClient, Nothing, ObjectiveListClient] = ZLayer.fromZIO {
    for {
      bc <- ZIO.service[BaseClient]
    } yield
      new ObjectiveListClient {
        override def checkSimilarity(country1: Country, country2: Country): IO[ObjectiveClientError, Score] = for {
          response <- bc.send(basicRequest.followRedirects(true).get(getUrl(country1))).mapError(e => ObjectiveClientError.ErrorFetchingStatistics(country1, e))
          _ <- ZIO.logDebug(response.toString())
        } yield Score(1)

        override def generateCountry: UIO[Country] = ZIO.succeed(Country.Sweden)
      }
  }


  def getUrl(country: Country): Uri =
    uri"https://objectivelists.com/${country.path}"

  sealed trait ObjectiveClientError

  object ObjectiveClientError {
    case class ErrorFetchingStatistics(country: Country, e: Throwable) extends ObjectiveClientError

  }

}
