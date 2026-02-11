name := """BookKeeperApp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.7.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "org.postgresql" % "postgresql" % "42.3.0"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.18.0"
libraryDependencies += "org.apache.commons" % "commons-csv" % "1.14.1"



// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
