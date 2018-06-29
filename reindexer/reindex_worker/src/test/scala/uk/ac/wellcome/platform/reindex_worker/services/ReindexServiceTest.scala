package uk.ac.wellcome.platform.reindex_worker.services

import javax.naming.ConfigurationException
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.amazonaws.services.sns.model.AmazonSNSException
import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.fixtures.ReindexServiceFixture
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ReindexRecord}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class ReindexServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with LocalDynamoDbVersioned
    with ExtendedPatience
    with ReindexServiceFixture {

  val shardName = "shard"
  val currentVersion = 1
  val desiredVersion = 2

  val exampleRecord = TestRecord(
    id = "id",
    version = 1,
    someData = "A ghastly gharial ganking a green golem.",
    reindexShard = shardName,
    reindexVersion = currentVersion
  )

  val exampleReindexJob = ReindexJob(
    shardId = "sierra/000",
    desiredVersion = 2
  )

  it("only sends notifications for records with a lower than desired reindexVersion") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexService(table, topic) { reindexService =>
          val newerRecord = exampleRecord.copy(
            id = "id1",
            reindexVersion = desiredVersion + 1
          )

          val olderRecord = exampleRecord.copy(
            id = "id2"
          )

          val records = List(
            newerRecord,
            olderRecord
          )

          records.foreach(record =>
            Scanamo.put(dynamoDbClient)(table.name)(record))

          val expectedRecords = List(
            ReindexRecord(
              id = olderRecord.id,
              version = olderRecord.version,
              reindexShard = olderRecord.reindexShard,
              reindexVersion = desiredVersion
            )
          )

          val reindexJob = ReindexJob(
            shardId = shardName,
            desiredVersion = desiredVersion
          )

          whenReady(reindexService.runReindex(reindexJob)) { _ =>
            val actualRecords: Seq[ReindexRecord] = listMessagesReceivedFromSNS(topic)
              .map { _.message }
              .map { fromJson[ReindexRecord](_).get }
              .distinct

            actualRecords should contain theSameElementsAs expectedRecords
          }
        }
      }
    }
  }

  it("sends notifications for records in the specified shard") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexService(table, topic) { reindexService =>
          val inShardRecords = List(
            exampleRecord.copy(id = "id1"),
            exampleRecord.copy(id = "id2")
          )

          val notInShardRecords = List(
            exampleRecord.copy(id = "id3", reindexShard = "not_the_same_shard"),
            exampleRecord.copy(id = "id4", reindexShard = "not_the_same_shard")
          )

          val reindexJob = ReindexJob(
            shardId = shardName,
            desiredVersion = desiredVersion
          )

          val recordList = inShardRecords ++ notInShardRecords

          recordList.foreach(record =>
            Scanamo.put(dynamoDbClient)(table.name)(record))

          val expectedRecords = inShardRecords.map { record =>
            ReindexRecord(
              id = record.id,
              version = record.version,
              reindexShard = shardName,
              reindexVersion = desiredVersion
            )
          }

          whenReady(reindexService.runReindex(reindexJob)) { _ =>
            val actualRecords: Seq[ReindexRecord] = listMessagesReceivedFromSNS(topic)
              .map { _.message }
              .map { fromJson[ReindexRecord](_).get }
              .distinct

            actualRecords should contain theSameElementsAs expectedRecords
          }
        }
      }
    }
  }

  it("returns a failed Future if the reindex is only partially successful") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexService(table, topic) { reindexService =>
          val inShardRecords = List(
            exampleRecord.copy(id = "id1"),
            exampleRecord.copy(id = "id2")
          )

          inShardRecords.foreach(record =>
            Scanamo.put(dynamoDbClient)(table.name)(record))

          // This record doesn't conform to our ReindexRecord type (it doesn't
          // have a version field), but it is in the same reindex shard as
          // the other two records -- so it will be picked up for reindexing,
          // but won't succeed.
          case class BadRecord(
                                id: String,
                                reindexShard: String,
                                reindexVersion: Int
                              )

          Scanamo.put(dynamoDbClient)(table.name)(
            BadRecord(
              id = "badId1",
              reindexShard = shardName,
              reindexVersion = currentVersion
            ))

          val reindexJob = ReindexJob(
            shardId = shardName,
            desiredVersion = desiredVersion
          )

          val future = reindexService.runReindex(reindexJob)
          whenReady(future.failed) {
            _ shouldBe a[GracefulFailureException]
          }
        }
      }
    }
  }

  it("returns a failed Future if there's a DynamoDB error") {
    withLocalSnsTopic { topic =>
      withReindexService(Table("does-not-exist", "no-such-index"), topic) { service =>
        val future = service.runReindex(exampleReindexJob)
        whenReady(future.failed) {
          _ shouldBe a[ResourceNotFoundException]
        }
      }
    }
  }

  it("returns a failed Future if there's an SNS error") {
    withLocalDynamoDbTable { table =>
      withReindexService(table, Topic("no-such-topic")) { service =>
        val reindexJob = ReindexJob(
          shardId = exampleRecord.reindexShard,
          desiredVersion = exampleRecord.reindexVersion + 1
        )

        Scanamo.put(dynamoDbClient)(table.name)(exampleRecord)

        val future = service.runReindex(reindexJob)
        whenReady(future.failed) {
          _ shouldBe a[AmazonSNSException]
        }
      }
    }
  }

  it("returns a failed Future if you don't specify a DynamoDB index") {
    withLocalSnsTopic { topic =>
      val service = new ReindexService(
        dynamoDbClient = dynamoDbClient,
        dynamoConfig = DynamoConfig(
          table = "mytable",
          maybeIndex = None
        ),
        snsWriter = new SNSWriter(
          snsClient = snsClient,
          snsConfig = SNSConfig(topicArn = topic.arn)
        )
      )

      val future = service.runReindex(exampleReindexJob)
      whenReady(future.failed) {
        _ shouldBe a[ConfigurationException]
      }
    }
  }

}
