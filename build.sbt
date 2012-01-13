name := "gt-engine"

organization := "kjetland"

version := "0.1.4"

javacOptions += "-g"

libraryDependencies ++= Seq(
	"org.codehaus.groovy" % "groovy" % "1.8.5",
	"commons-collections" % "commons-collections" % "3.2.1",
	"commons-lang" % "commons-lang" % "2.6",
	"org.easytesting" % "fest-assert" % "1.4" % "test",
	"com.novocode" % "junit-interface" % "0.7" % "test")
