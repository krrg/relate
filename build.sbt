lazy val Benchmark = config("bench") extend Test

lazy val Regression = config("regression") extend Benchmark

lazy val buildSettings = Seq(
  organization := "com.lucidchart",
  version := "1.14.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.10.6"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:higherKinds"
  )
)

lazy val publishingSettings = Seq(
  pgpPassphrase := Some(Array()),
  pgpPublicRing := file(System.getProperty("user.home")) / ".pgp" / "pubring",
  pgpSecretRing := file(System.getProperty("user.home")) / ".pgp" / "secring",
  pomExtra := (
    <url>https://github.com/lucidsoftware/relate</url>
    <licenses>
      <license>
      <name>Apache License</name>
      <url>http://www.apache.org/licenses/</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:lucidsoftware/relate.git</url>
      <connection>scm:git:git@github.com:lucidsoftware/relate.git</connection>
    </scm>
    <developers>
      <developer>
        <id>msiebert</id>
        <name>Mark Siebert</name>
      </developer>
      <developer>
        <id>gregghz</id>
        <name>Gregg Hernandez</name>
      </developer>
      <developer>
        <id>matthew-lucidchart</id>
        <name>Matthew Barlocker</name>
      </developer>
      <developer>
        <id>pauldraper</id>
        <name>Paul Draper</name>
      </developer>
    </developers>
  ),
  pomIncludeRepository := { _ => false },
  publishMavenStyle := true,
  credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", System.getenv("SONATYPE_USERNAME"), System.getenv("SONATYPE_PASSWORD")),
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val macroSettings = buildSettings ++ Seq(
  moduleName := "relate-macros",
  scalacOptions += "-language:experimental.macros",
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.3.1" % "test",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.specs2" %% "specs2-core" % "3.8.4" % "test",
    "org.specs2" %% "specs2-mock" % "3.8.4" % "test",
    "org.typelevel" %% "macro-compat" % "1.1.1"
  )
)

lazy val macros = project.in(file("macros"))
  .settings(publishingSettings)
  .settings(macroSettings)
  .dependsOn(relate)

lazy val relate = project.in(file("relate"))
  .settings(publishingSettings)
  .settings(Defaults.coreDefaultSettings)
  .settings(buildSettings)
  .settings(
    name := "Relate",
    moduleName := "relate",
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2" % "2.3.12" % "test",
      "com.h2database" % "h2" % "1.4.191" % "test",
      "com.storm-enroute" %% "scalameter" % "0.7" % "bench",
      "com.storm-enroute" %% "scalameter" % "0.7" % "regression"
    ),
    libraryDependencies <+= (scalaVersion) { sv =>
      sv match {
        case x if x.startsWith("2.10") =>
          "com.typesafe.play" %% "anorm" % "2.4.0" % "bench"
        case _ =>
          "com.typesafe.play" %% "anorm" % "2.5.2" % "bench"
      }
    },
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    parallelExecution in Benchmark := false,
    parallelExecution in Regression := false,
    logBuffered := false
  )
  .configs(Benchmark).settings(inConfig(Benchmark)(Defaults.testSettings): _*)
  .configs(Regression).settings(inConfig(Regression)(Defaults.testSettings): _*)

lazy val root = project.in(file(".")).aggregate(relate, macros)