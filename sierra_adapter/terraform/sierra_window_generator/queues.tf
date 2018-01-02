module "windows_queue" {
  source      = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v1.1.0"
  queue_name  = "sierra_${var.resource_type}_windows_queue"
  aws_region  = "${var.aws_region}"
  account_id  = "${var.account_id}"
  topic_names = ["${module.topic_sierra_windows.name}"]

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}
