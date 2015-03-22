import sbt._

object EventStorePlugin {
  val esDir = settingKey[File]("directory of eventstore")
  val downloadES= taskKey[File]("download eventstore from geteventstore.com")
  val deleteES = taskKey[Unit]("Delete eventstore")

  val startES = taskKey[Unit]("start eventstore")
  val stopES = taskKey[Unit]("stop eventstore")

  val settings = Seq(
    esDir := file("EventStore-OSS-Linux-v3.0.3"),
    downloadES := {
      if (!esDir.value.exists()) {
        println("Downloading eventstore")
        Process("./getEventStore.sh").!
      } else {
        println("Eventstore already downloaded")
      }

      esDir.value
    },
    deleteES := {
      println("Deleting eventstore")
      IO.delete(esDir.value)
    },
    startES := {
      val startScript: File = esDir.value / "start.sh"
      IO.write(startScript, "./run-node.sh --mem-db --run-projections All &")
      startScript.setExecutable(true)

      Process("./start.sh", esDir.value).lines_!.foreach(println)
    },
    stopES := {
      "killall clusternode" !
    }
  )
}