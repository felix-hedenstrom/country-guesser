Global / onChangedBuildSource := IgnoreSourceChanges // not working well with webpack devserver

name                     := "OutwatchExample"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

val versions = new {
  val outwatch  = "1.0.0-RC14"
  val scalaTest = "3.2.15"
  val zio = "2.0.9"
  val scalaTime = "2.2.1"
}

// do not warn about unused setting key. TODO: why is this needed? scala-js-bundler bug? sbt says this setting is unused, but it is used.
Global / excludeLintKeys += webpackDevServerPort

lazy val scalaJsMacrotaskExecutor = Seq(
  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies       += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.0.0",
  Compile / npmDependencies += "setimmediate"  -> "1.0.5", // polyfill
)

def readJsDependencies(baseDirectory: File, field: String): Seq[(String, String)] = {
  val packageJson = ujson.read(IO.read(new File(s"$baseDirectory/package.json")))
  packageJson(field).obj.mapValues(_.str).toSeq
}

lazy val webapp = project
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
  )
  .settings(scalaJsMacrotaskExecutor)
  .settings(
    libraryDependencies          ++= Seq(
      "io.github.outwatch"  %%% "outwatch"  % versions.outwatch,
      "com.github.cornerman" %%% "colibri-zio" % "0.7.8", // zio support
      "dev.zio"             %%% "zio"       % versions.zio,
      "dev.zio"             %%% "zio-managed" % versions.zio,
      "dev.zio" %%% "zio-interop-cats" % "23.0.0.2",
      "org.scalatest"       %%% "scalatest" % versions.scalaTest % Test,
      "io.github.cquiroz" %%% "scala-java-time" % versions.scalaTime,
      "io.github.cquiroz" %%% "scala-java-time-tzdb" % versions.scalaTime,
      "com.softwaremill.sttp.client3" %%% "zio" % "3.8.11"
    ),
    Compile / npmDependencies    ++= readJsDependencies(baseDirectory.value, "dependencies"),
    Compile / npmDevDependencies ++= readJsDependencies(baseDirectory.value, "devDependencies"),
    scalacOptions --= Seq(
      "-Xfatal-warnings",
    ), // overwrite option from https://github.com/DavidGregory084/sbt-tpolecat

    useYarn       := true, // Makes scalajs-bundler use yarn instead of npm
    yarnExtraArgs += "--prefer-offline",
    scalaJSLinkerConfig ~= (_.withModuleKind(
      ModuleKind.CommonJSModule,
    )), // configure Scala.js to emit a JavaScript module instead of a top-level script
    scalaJSUseMainModuleInitializer   := true, // On Startup, call the main function
    webpackDevServerPort              := sys.env
      .get("FRONTEND_PORT")
      .flatMap(port => scala.util.Try(port.toInt).toOption)
      .getOrElse(12345),
    webpackDevServerExtraArgs         := Seq("--color"),
    webpack / version                 := "5.75.0",
    webpackCliVersion                 := "5.0.0",
    startWebpackDevServer / version   := "4.11.1",
    webpackDevServerExtraArgs         := Seq("--color"),
    fullOptJS / webpackEmitSourceMaps := true,
    fastOptJS / webpackBundlingMode   := BundlingMode.LibraryOnly(),
    fastOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.dev.js"),
    fullOptJS / webpackConfigFile     := Some(baseDirectory.value / "webpack.config.prod.js"),
    Test / requireJsDomEnv            := true,
  )

addCommandAlias("prod", "fullOptJS/webpack")
addCommandAlias("dev", "devInit; devWatchAll; devDestroy")
addCommandAlias("devInit", "; webapp/fastOptJS/startWebpackDevServer")
addCommandAlias("devWatchAll", "~; webapp/fastOptJS/webpack")
addCommandAlias("devDestroy", "webapp/fastOptJS/stopWebpackDevServer")
