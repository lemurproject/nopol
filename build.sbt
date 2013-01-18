import AssemblyKeys._

name := "nopol"

version := "0.1"

libraryDependencies += "org.apache.commons" % "commons-compress" % "1.4.1" withSources ()

libraryDependencies += "commons-io" % "commons-io" % "2.4"

// Include only src/main/java in the compile configuration
unmanagedSourceDirectories in Compile <<= Seq(javaSource in Compile).join

// Include only src/test/java in the test configuration
unmanagedSourceDirectories in Test <<= Seq(javaSource in Test).join

// Enable sbt-assembly
assemblySettings

assembleArtifact in packageScala := false

