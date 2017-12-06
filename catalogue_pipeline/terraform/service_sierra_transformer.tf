module "sierra_transformer" {
  source = "git::https://github.com/wellcometrust/terraform-modules.git//sqs_autoscaling_service?ref=ref=v2.0.0"
  name   = "sierra_transformer"

  source_queue_name  = "${module.sierra_transformer_queue.name}"
  source_queue_arn   = "${module.sierra_transformer_queue.arn}"
  ecr_repository_url = "${module.ecr_repository_transformer.repository_url}"
  release_id         = "${var.release_ids["transformer"]}"
  config_template    = "transformer"

  config_vars = {
    sns_arn              = "${module.id_minter_topic.arn}"
    transformer_queue_id = "${module.sierra_transformer_queue.id}"
    source_table_name    = "SierraData"
    metrics_namespace    = "sierra-transformer"
  }

  alb_priority = "108"

  cluster_name               = "${aws_ecs_cluster.services.name}"
  vpc_id                     = "${module.vpc_services.vpc_id}"
  alb_cloudwatch_id          = "${module.services_alb.cloudwatch_id}"
  alb_listener_https_arn     = "${module.services_alb.listener_https_arn}"
  alb_listener_http_arn      = "${module.services_alb.listener_http_arn}"
  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
}
