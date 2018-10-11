package uk.ac.wellcome.platform.archive.registrar.fixtures

import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  BagId,
  BagLocation
}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{
  ConfigModule,
  TestAppConfigModule,
  VHSModule
}
import uk.ac.wellcome.platform.archive.registrar.{Registrar => RegistrarApp}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{
  LocalDynamoDb,
  LocalVersionedHybridStore,
  S3
}
import uk.ac.wellcome.test.fixtures.TestWith

trait Registrar
    extends S3
    with Messaging
    with LocalVersionedHybridStore
    with BagLocationFixtures
    with LocalDynamoDb {

  def sendNotification(requestId: UUID,
                       bagId: BagId,
                       bagLocation: BagLocation,
                       queuePair: QueuePair) =
    sendNotificationToSQS(
      queuePair.queue,
      ArchiveComplete(requestId, bagId, bagLocation)
    )

  def withBagNotification[R](
    requestId: UUID,
    bagId: BagId,
    queuePair: QueuePair,
    storageBucket: Bucket,
    dataFileCount: Int = 1)(testWith: TestWith[BagLocation, R]) = {
    withBag(storageBucket, dataFileCount) { bagLocation =>
      sendNotification(requestId, bagId, bagLocation, queuePair)
      testWith(bagLocation)
    }
  }

  override def createTable(table: Table) = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(
          new KeySchemaElement()
            .withAttributeName("id")
            .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("id")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
    )

    table
  }

  def withApp[R](storageBucket: Bucket,
                 hybridStoreBucket: Bucket,
                 hybridStoreTable: Table,
                 queuePair: QueuePair,
                 ddsTopic: Topic,
                 progressTopic: Topic)(testWith: TestWith[RegistrarApp, R]) = {

    class TestApp extends Logging {

      val appConfigModule = new TestAppConfigModule(
        queuePair.queue.url,
        storageBucket.name,
        ddsTopic.arn,
        progressTopic.arn,
        hybridStoreTable.name,
        hybridStoreBucket.name,
        "archive"
      )

      val injector: Injector = Guice.createInjector(
        appConfigModule,
        ConfigModule,
        VHSModule,
        AkkaModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSClientModule,
        S3ClientModule,
        DynamoClientModule,
        MessageStreamModule
      )

      val app = injector.getInstance(classOf[RegistrarApp])

    }

    testWith((new TestApp()).app)
  }

  def withRegistrar[R](
    testWith: TestWith[
      (Bucket, QueuePair, Topic, Topic, RegistrarApp, Bucket, Table),
      R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic {
        ddsSnsTopic =>
          withLocalSnsTopic {
            progressTopic =>
              withLocalS3Bucket {
                storageBucket =>
                  withLocalS3Bucket {
                    hybridStoreBucket =>
                      withLocalDynamoDbTable { hybridDynamoTable =>
                        withApp(
                          storageBucket,
                          hybridStoreBucket,
                          hybridDynamoTable,
                          queuePair,
                          ddsSnsTopic,
                          progressTopic) { registrar =>
                          testWith(
                            (
                              storageBucket,
                              queuePair,
                              ddsSnsTopic,
                              progressTopic,
                              registrar,
                              hybridStoreBucket,
                              hybridDynamoTable)
                          )
                        }
                      }
                  }

              }
          }
      }
    })
  }

  def withRegistrarAndBrokenVHS[R](
    testWith: TestWith[(Bucket, QueuePair, Topic, Topic, RegistrarApp, Bucket),
                       R]) = {
    withLocalSqsQueueAndDlqAndTimeout(5)(queuePair => {
      withLocalSnsTopic {
        ddsSnsTopic =>
          withLocalSnsTopic {
            progressTopic =>
              withLocalS3Bucket {
                storageBucket =>
                  withLocalS3Bucket { hybridStoreBucket =>
                    withApp(
                      storageBucket,
                      hybridStoreBucket,
                      Table("does-not-exist", ""),
                      queuePair,
                      ddsSnsTopic,
                      progressTopic) { registrar =>
                      testWith(
                        (
                          storageBucket,
                          queuePair,
                          ddsSnsTopic,
                          progressTopic,
                          registrar,
                          hybridStoreBucket)
                      )
                    }
                  }
              }

          }
      }
    })
  }

}
