name := "Captor"

version := "1.0"

scalaVersion := "2.10.4"


libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.4.0" % "provided"

libraryDependencies += "org.apache.spark" % "spark-yarn_2.10" % "1.4.0" % "provided"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.4.0" % "provided"

libraryDependencies += "com.github.scopt" % "scopt_2.10" % "3.3.0"

libraryDependencies += "org.jsoup" % "jsoup" % "1.8.2"

libraryDependencies += "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.4"

libraryDependencies += "org.apache.hive" % "hive-jdbc" % "0.13.1"

