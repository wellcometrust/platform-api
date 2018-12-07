module "service" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/prebuilt/scaling?ref=8037d31e4277fdd253b57182c4a54ca312b624be"

  service_name    = "${var.service_name}"
  container_image = "${var.container_image}"

  vpc_id  = "${var.vpc_id}"
  subnets = "${var.subnets}"

  namespace_id = "${var.namespace_id}"

  cluster_id   = "${var.cluster_id}"
  cluster_name = "${var.cluster_name}"

  service_egress_security_group_id = "${var.service_egress_security_group_id}"

  cpu    = "${var.cpu}"
  memory = "${var.memory}"

  security_group_ids = ["${var.security_group_ids}"]

  metric_namespace = "${var.metric_namespace}"
  high_metric_name = "${var.high_metric_name}"

  env_vars        = "${var.env_vars}"
  env_vars_length = "${var.env_vars_length}"

  min_capacity = "${var.min_capacity}"
  max_capacity = "${var.max_capacity}"

  desired_task_count = "${var.desired_task_count}"

  launch_type = "${var.launch_type}"
}