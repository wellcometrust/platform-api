module "grafana" {
  source           = "./services"
  name             = "grafana"
  cluster_id       = "${aws_ecs_cluster.monitoring.id}"
  task_role_arn    = "${module.ecs_grafana_iam.task_role_arn}"
  vpc_id           = "${module.vpc_monitoring.vpc_id}"
  listener_arn     = "${module.monitoring_alb.listener_arn}"
  healthcheck_path = "/api/health"
  volume_name      = "grafana"
  volume_host_path = "${module.monitoring_userdata.efs_mount_directory}/grafana"
  container_name   = "grafana"
  container_port   = "3000"

  template_name  = "single_image_with_volume"
  docker_image   = "grafana/grafana"
  container_path = "/var/lib/grafana"

  environment_vars = <<EOF
  [
    {"name" : "GF_AUTH_ANONYMOUS_ENABLED", "value" : "${var.grafana_anonymous_enabled}"},
    {"name" : "GF_AUTH_ANONYMOUS_ORG_ROLE", "value" : "${var.grafana_anonymous_role}"},
    {"name" : "GF_SECURITY_ADMIN_USER", "value" : "${var.grafana_admin_user}"},
    {"name" : "GF_SECURITY_ADMIN_PASSWORD", "value" : "${var.grafana_admin_password}"}
  ]
  EOF

  config_key        = ""
  infra_bucket      = ""
  is_config_managed = false
}

module "grafana_efs" {
  name                         = "grafana"
  source                       = "./efs"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
  subnets                      = "${module.vpc_monitoring.subnets}"
  efs_access_security_group_id = "${module.monitoring_cluster_asg.instance_sg_id}"
}
