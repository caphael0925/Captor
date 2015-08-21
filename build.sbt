
name := "Captor"

version := "0.10.0"

scalaVersion := "2.10.4"

val akkaVersion = "2.3.4"
val sprayVersion = "1.3.1"

resolvers ++=
  Seq("repo" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.4.1"

libraryDependencies += "org.apache.spark" % "spark-yarn_2.10" % "1.4.1"

libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.4.1"

libraryDependencies += "com.github.scopt" % "scopt_2.10" % "3.3.0"

libraryDependencies += "org.jsoup" % "jsoup" % "1.8.2"

libraryDependencies += "com.github.detro.ghostdriver" % "phantomjsdriver" % "1.1.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10"

//libraryDependencies += "com.typesafe.akka" % "akka-actor" % akkaVersion

//libraryDependencies += "com.typesafe.akka" % "akka-slf4j" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion

libraryDependencies += "org.apache.hive" % "hive-jdbc" % "0.13.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

//libraryDependencies += "org.scalatest" % "scalatest" % "2.2.0"

//libraryDependencies ++= {
//  Seq(
//    "io.spray" %% "spray-can" % sprayVersion,
//    "io.spray" %% "spray-routing" % sprayVersion,
//    "io.spray" %% "spray-json" % "1.2.6"
//  )
//}
