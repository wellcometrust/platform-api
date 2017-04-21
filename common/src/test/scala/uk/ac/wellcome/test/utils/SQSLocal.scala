package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.{PurgeQueueRequest, SetQueueAttributesRequest}
import org.scalatest.{BeforeAndAfterEach, Suite}
import scala.collection.JavaConversions._

trait SQSLocal extends Suite with BeforeAndAfterEach {

  val sqsClient = AmazonSQSClientBuilder
    .standard()
    .withCredentials(
      new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
    .withEndpointConfiguration(
      new EndpointConfiguration(s"http://localhost:9324", "localhost"))
    .build()

  val idMinterQueue = "es_id_minter_queue"
  val idMinterQueueUrl = sqsClient.createQueue(idMinterQueue).getQueueUrl
  // AWS does not delete a message automatically once it's read.
  // It hides for the number of seconds specified in VisibilityTimeout.
  // After the timeout has passet it will be sent again.
  // Setting 1 second timeout for tests, to be able to test message deletion
  sqsClient.setQueueAttributes(idMinterQueueUrl, Map("VisibilityTimeout"->"1"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    sqsClient.purgeQueue(
      new PurgeQueueRequest().withQueueUrl(idMinterQueueUrl))
  }
}
