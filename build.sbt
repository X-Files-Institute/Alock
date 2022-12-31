name := "Alock"

version := "0.1"

scalaVersion := "3.2.1"

lazy val akkaVersion                    = "2.7.0"
lazy val akkaHttpVersion                = "10.1.7"
lazy val akkaPersistenceInmemoryVersion = "2.5.15.1"
lazy val scalaTestVersion               = "3.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-testkit"       % akkaVersion % Test,
  "org.scalatest"            %% "scalatest"          % "3.2.14"    % "test",
  "com.typesafe.akka"        %% "akka-persistence"   % akkaVersion,
  "org.iq80.leveldb"          % "leveldb"            % "0.12",
  "org.fusesource.leveldbjni" % "leveldbjni-all"     % "1.8",
  "com.typesafe.akka"        %% "akka-actor"         % akkaVersion,
  "com.typesafe.akka"        %% "akka-stream"        % akkaVersion,
  "com.typesafe.akka"        %% "akka-cluster"       % akkaVersion,
  "com.typesafe.akka"        %% "akka-cluster-tools" % akkaVersion
)

Test / fork := true
