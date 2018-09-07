locals {
  namespace                 = "archive-storage"
  lambda_error_alarm_arn    = "${data.terraform_remote_state.shared_infra.lambda_error_alarm_arn}"
  dlq_alarm_arn             = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id                    = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets           = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  archive_bucket_name       = "wellcomecollection-assets-archive-storage"
  ingest_bucket_name        = "wellcomecollection-assets-archive-ingest"
  archivist_container_image = "${module.ecr_repository_archivist.repository_url}:${var.release_ids["archivist"]}"
  registrar_container_image = "${module.ecr_repository_registrar.repository_url}:${var.release_ids["registrar"]}"
  bagger_container_image = "${module.ecr_repository_bagger.repository_url}:${var.release_ids["bagger"]}"
}
