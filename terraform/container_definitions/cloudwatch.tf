resource "aws_cloudwatch_log_group" "task" {
  name = "platform/${var.name}"
}
