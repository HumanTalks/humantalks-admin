name := """humantalks-backend-orga"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.jcenterRepo,
  "Atlassian Releases" at "https://maven.atlassian.com/public/"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  "com.iheart" %% "ficus" % "1.2.7",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
  "org.jsoup" % "jsoup" % "1.9.2",
  "com.github.spullara.mustache.java" % "compiler" % "0.9.2",
  "org.webjars" % "jquery" % "1.12.4",
  "org.webjars" % "bootstrap" % "3.3.7-1",
  "org.webjars" % "font-awesome" % "4.6.3",
  "org.webjars.npm" % "select2" % "4.0.3",
  "org.webjars.npm" % "select2-bootstrap-theme" % "0.1.0-beta.9",
  "org.webjars" % "bootstrap-datetimepicker" % "2.3.8",
  "org.webjars" % "typeaheadjs" % "0.11.1",
  "org.webjars" % "jquery-throttle-debounce-plugin" % "1.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % Test,
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % Test
)

play.sbt.routes.RoutesKeys.routesImport ++= Seq(
  "com.humantalks.auth.entities.AuthToken",
  "com.humantalks.internal.persons.Person",
  "com.humantalks.internal.partners.Partner",
  "com.humantalks.internal.talks.Talk",
  "com.humantalks.internal.events.Event",
  "com.humantalks.internal.admin.config.Config")

lazy val root =
  (project in file("."))
    .enablePlugins(PlayScala)
    .enablePlugins(BuildInfoPlugin)
    .enablePlugins(SbtWeb)
    .settings(
      scalacOptions ++= Seq(
        "-Xlint", // Enable or disable specific warnings: `_' for all, `-Xlint:help' to list
        //"-Xfatal-warnings", // Fail the compilation if there are any warnings.
        "-deprecation", // Emit warning and location for usages of deprecated APIs.
        "-feature", // Emit warning and location for usages of features that should be imported explicitly.
        "-encoding", "UTF-8", // Specify character encoding used by source files.
        "-unchecked", // Enable additional warnings where generated code depends on assumptions.
        "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
        "-Ywarn-dead-code", // Warn when dead code is identified. (N.B. doesn't work well with the ??? hole)
        "-Ywarn-numeric-widen", // Warn when numerics are widened.
        "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
        "-Xfuture", // Turn on future language features.
        "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are are unused.
        //"-Ywarn-unused-import", // Warn when imports are unused.
        "-Ydelambdafy:method", // Strategy used for translating lambdas into JVM code. (inline,method) default:inline
        "-Ybackend:GenBCode", // Choice of bytecode emitter. (GenASM,GenBCode) default:GenASM
        "-target:jvm-1.8" // Target platform for object files. All JVM 1.5 targets are deprecated. (jvm-1.5,jvm-1.6,jvm-1.7,jvm-1.8)
      ),
      buildInfoKeys := Seq[BuildInfoKey](
        name, version, scalaVersion, sbtVersion,
        "gitHash" -> "TODO"/*new java.lang.Object(){
          // http://stackoverflow.com/questions/26671073/can-the-runtime-of-a-heroku-app-know-its-commit-id
          override def toString(): String = {
            try {
              val extracted = new java.io.InputStreamReader(java.lang.Runtime.getRuntime().exec("git rev-parse --short HEAD").getInputStream())
              (new java.io.BufferedReader(extracted)).readLine()
            } catch {
              case t: Throwable => "get git hash failed"
            }
          }
        }.toString()*/
      ),
      buildInfoPackage := "global",
      buildInfoOptions := Seq(BuildInfoOption.BuildTime)
    )

pipelineStages := Seq(uglify, digest, gzip)

routesGenerator := InjectedRoutesGenerator
