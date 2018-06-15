module "data_science_experiments_userdata" {
  source       = "git::https://github.com/wellcometrust/terraform.git//userdata?ref=v1.0.0"
  cluster_name = "${aws_ecs_cluster.data_science_experiments.name}"
}
