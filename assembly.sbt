import AssemblyKeys._ // put this at the top of the file

assemblySettings

mergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.last
}