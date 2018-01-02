module "dynamo_to_sns" {
  source = "git::https://github.com/wellcometrust/platform.git//shared_infra/dynamo_to_sns?ref=nofilter-dynamo_to_sns"

  name           = "sierra_merger_dynamo_to_sns_${var.resource_type}"
  src_stream_arn = "${aws_dynamodb_table.sierra_table.stream_arn}"
  dst_topic_arn  = "${module.topic_sierra_updates.arn}"

  stream_view_type = "NEW_IMAGE_ONLY"

  lambda_error_alarm_arn = "${var.lambda_error_alarm_arn}"
}
