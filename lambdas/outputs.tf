output "lambda_error_alarm_arn" {
  value = "${module.lambda_error_alarm.arn}"
}

output "run_ecs_task_topic_arn" {
  value = "${module.run_ecs_task.arn}"
}

output "run_ecs_task_topic_publish_policy" {
  value = "${module.run_ecs_task.publish_policy}"
}

output "iam_policy_document_describe_services" {
  value = "${data.aws_iam_policy_document.describe_services.json}"
}
