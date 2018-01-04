module "bibs_pipeline" {
  source = "pipeline"

  resource_type = "bibs"

  sierra_to_dynamo_release_id = "${var.release_ids["sierra_bibs_to_dynamo"]}"
  sierra_merger_release_id    = "${var.release_ids["sierra_bib_merger"]}"

  merged_dynamo_table_name = "${aws_dynamodb_table.sierradata_table.name}"

  window_length_minutes    = 30
  trigger_interval_minutes = 15

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_items_fields}"

  cluster_name = "${module.sierra_adapter_cluster.cluster_name}"
  vpc_id       = "${module.vpc_sierra_adapter.vpc_id}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}

module "items_pipeline" {
  source = "pipeline"

  resource_type = "items"

  sierra_to_dynamo_release_id = "${var.release_ids["sierra_items_to_dynamo"]}"
  sierra_merger_release_id    = "${var.release_ids["sierra_item_merger"]}"

  merged_dynamo_table_name = "${aws_dynamodb_table.sierradata_table.name}"

  window_length_minutes    = 30
  trigger_interval_minutes = 15

  sierra_api_url      = "${var.sierra_api_url}"
  sierra_oauth_key    = "${var.sierra_oauth_key}"
  sierra_oauth_secret = "${var.sierra_oauth_secret}"
  sierra_fields       = "${var.sierra_items_fields}"

  cluster_name = "${module.sierra_adapter_cluster.cluster_name}"
  vpc_id       = "${module.vpc_sierra_adapter.vpc_id}"

  alb_server_error_alarm_arn = "${local.alb_server_error_alarm_arn}"
  alb_client_error_alarm_arn = "${local.alb_client_error_alarm_arn}"
  alb_cloudwatch_id          = "${module.sierra_adapter_cluster.alb_cloudwatch_id}"
  alb_listener_http_arn      = "${module.sierra_adapter_cluster.alb_listener_http_arn}"
  alb_listener_https_arn     = "${module.sierra_adapter_cluster.alb_listener_https_arn}"

  dlq_alarm_arn          = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  account_id = "${data.aws_caller_identity.current.account_id}"
}
