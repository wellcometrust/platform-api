package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig


class VerifiedBagUploaderTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with AkkaS3 {

  import BagItUtils._

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  it("succeeds when verifying and uploading a valid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val _ = s3AkkaClient

        val bagUploaderConfig = BagUploaderConfig(uploadNamespace = storageBucket.name)
        val uploader = new VerifiedBagUploader(bagUploaderConfig)

        val bagName = randomAlphanumeric()
        val zipFile = createBagItZip(bagName, 1)

        val verification = uploader.verify(zipFile, bagName)

        whenReady(verification) { _ =>
          // Do nothing
        }
      }
    }
  }

  it("fails when verifying and uploading an invalid bag") {
    withLocalS3Bucket { storageBucket =>
      withS3AkkaClient(system, materializer) { s3AkkaClient =>
        implicit val _ = s3AkkaClient

        val bagUploaderConfig = BagUploaderConfig(uploadNamespace = storageBucket.name)
        val uploader = new VerifiedBagUploader(bagUploaderConfig)

        val bagName = randomAlphanumeric()
        val zipFile = createBagItZip(bagName, 1, false)

        val verification = uploader.verify(zipFile, bagName)

        whenReady(verification.failed) { e =>
          println(e)
          // Do nothing
        }
      }
    }
  }
}
