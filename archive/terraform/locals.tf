locals {
  namespace                      = "archive-storage"
  lambda_error_alarm_arn         = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn                  = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                         = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  public_subnets                 = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
  private_subnets                = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  archive_bucket_name            = "wellcomecollection-assets-archive-storage"
  ingest_bucket_name             = "wellcomecollection-assets-archive-ingest"
  archivist_container_image      = "${module.ecr_repository_archivist.repository_url}:${var.release_ids["archivist"]}"
  registrar_container_image      = "${module.ecr_repository_registrar.repository_url}:${var.release_ids["registrar"]}"
  progress_async_container_image = "${module.ecr_repository_progress_async.repository_url}:${var.release_ids["progress_async"]}"
  progress_http_container_image  = "${module.ecr_repository_progress_http.repository_url}:${var.release_ids["progress_http"]}"
  notifier_container_image       = "${module.ecr_repository_notifier.repository_url}:${var.release_ids["notifier"]}"
  callback_stub_server_container_image = "${module.ecr_repository_callback_stub_server.repository_url}:${var.release_ids["callback_stub_server"]}"
  bagger_container_image         = "${module.ecr_repository_bagger.repository_url}:${var.release_ids["bagger"]}"
  api_ecs_container_image        = "${module.ecr_repository_archive_api.repository_url}:${var.release_ids["archive_api"]}"
}
