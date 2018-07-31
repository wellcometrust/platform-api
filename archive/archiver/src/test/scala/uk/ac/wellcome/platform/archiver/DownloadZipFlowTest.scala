package uk.ac.wellcome.platform.archiver

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archiver.flow.DownloadZipFlow
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.JavaConverters._

class DownloadZipFlowTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with AkkaS3 {

  import BagItUtils._

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("downloads a zipfile from s3") {

    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val _ = s3AkkaClient

        val downloadZipFlow = DownloadZipFlow()

        val fileName = randomAlphanumeric()
        val bagName = randomAlphanumeric()
        val zipFile = createBagItZip(bagName, 1)

        val file = new File(zipFile.getName)
        s3Client.putObject(storageBucket.name, fileName, file)

        val objectLocation = ObjectLocation(storageBucket.name, fileName)

        val download = downloadZipFlow.runWith(Source.single(objectLocation), Sink.head)._2

        whenReady(download) { downloadedZipFile =>
          zipFile.entries.asScala.toList.map(_.toString) should contain theSameElementsAs downloadedZipFile.entries.asScala.toList.map(_.toString)
          zipFile.size shouldEqual downloadedZipFile.size
        }
      }
    }
  }

}
