// Scalate template engine config for Xitrum
// "import" must be at top of build.sbt, or SBT will complain
import ScalateKeys._

import de.johoop.jacoco4sbt._

import JacocoPlugin._

// Precompile Scalate
seq(scalateSettings:_*)

scalateTemplateConfig in Compile := Seq(TemplateConfig(
  file("src") / "main" / "scalate",
  Seq(),
  Seq(Binding("helper", "xitrum.Action", true))
))

libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "1.9"

//------------------------------------------------------------------------------

organization := "de.fraunhofer.scai"

name         := "scaltex"

version      := "0.5.0-SNAPSHOT"

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// Most Scala projects are published to Sonatype, but Sonatype is not default
// and it takes several hours to sync from Sonatype to Maven Central
resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "tv.cntt" %% "xitrum" % "3.13"

// Xitrum uses SLF4J, an implementation of SLF4J is needed
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

// xgettext i18n translation key string extractor is a compiler plugin ---------

autoCompilerPlugins := true

addCompilerPlugin("tv.cntt" %% "xgettext" % "1.0")

scalacOptions += "-P:xgettext:xitrum.I18n"

// Put config directory in classpath for easier development --------------------

// For "sbt console"
unmanagedClasspath in Compile <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") }

// For "sbt run"
unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") }

// Copy these to target/xitrum when sbt xitrum-package is run
XitrumPackage.copy("config", "public", "script")

// Add scala test and akka testkit
libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.1.6" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.2" % "test"

// dijon as json library
resolvers += "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.pathikrit" %% "dijon" % "0.2.4"

// Enable code coverage
jacoco.settings

// apache commons
// libraryDependencies += "org.apache.commons" % "commons-lang3"  % "3.3.1"

// curl for scala
libraryDependencies += "com.m3" %% "curly-scala" % "0.5.4"
