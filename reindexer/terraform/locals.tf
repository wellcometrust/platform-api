locals {
  vhs_dynamodb_full_access_policy = "${data.terraform_remote_state.catalogue_pipeline.vhs_dynamo_full_access_policy}"
  vhs_dynamodb_update_policy      = "${data.terraform_remote_state.catalogue_pipeline.vhs_dynamodb_update_policy}"
  vhs_table_name                  = "${data.terraform_remote_state.catalogue_pipeline.vhs_table_name}"
  vhs_table_stream_arn            = "${data.terraform_remote_state.catalogue_pipeline.vhs_table_stream_arn}"

  lambda_error_alarm_arn = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
}
