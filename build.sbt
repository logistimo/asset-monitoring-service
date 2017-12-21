import com.github.play2war.plugin._

name := "asset-monitoring-service"

version := "1.0"

lazy val `asset-management-service` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.10.4"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  ws,
  javaJdbc,
  cache,
  javaWs,
  javaJpa,
  javaJpa.exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api"),
  "org.hibernate" % "hibernate-core" % "4.3.6.Final",
  "org.quartz-scheduler" % "quartz" % "2.2.0",
  "mysql" % "mysql-connector-java" % "5.1.36",
  "commons-collections" % "commons-collections" % "3.2.1",
  "com.google.code.gson" % "gson" % "2.2",
  "org.hibernate" % "hibernate-commons-annotations" % "3.2.0.Final",
  "org.hibernate" % "hibernate-core" % "4.3.6.Final",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.6.Final",
  "org.hibernate" % "hibernate-validator" % "5.0.3.Final",
  "com.typesafe.akka" %% "akka-camel" % "2.3.10",
  "org.apache.camel" % "camel-jetty" % "2.14.0",
  "org.apache.camel" % "camel-quartz" % "2.14.0",
  "org.apache.camel" % "camel-core" % "2.14.0",
  "org.apache.camel" % "camel-jms" % "2.14.0",
  "org.apache.activemq" % "activemq-core" % "5.7.0",
  "org.apache.activemq" % "activemq-camel" % "5.7.0",
  "org.slf4j" % "slf4j-api" % "1.7.2",
  "ch.qos.logback" % "logback-classic" % "1.0.7",
  "org.glassfish.web" % "el-impl" % "2.2.1-b05",
  "javax.el" % "el-api" % "2.2.1-b04",
  "javax.validation" % "validation-api" % "1.1.0.Final",
  "org.json" % "json" % "20090211",
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "redis.clients" % "jedis" % "2.6.2",
  "commons-pool" % "commons-pool" % "1.6",
  "io.dropwizard.metrics" % "metrics-core" % "3.2.5",
  "io.dropwizard.metrics" % "metrics-healthchecks" % "3.2.5"
)

Play2WarPlugin.play2WarSettings

Play2WarKeys.servletVersion := "2.5"

Play2WarKeys.explodedJar := true