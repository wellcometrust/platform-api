module "services_alb" {
  source  = "./ecs_alb"
  name    = "services"
  subnets = ["${module.vpc_services.subnets}"]

  loadbalancer_security_groups = [
    "${module.services_cluster_asg.loadbalancer_sg_https_id}",
    "${module.services_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "services.wellcomecollection.org"
  vpc_id             = "${module.vpc_services.vpc_id}"

  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
}

module "monitoring_alb" {
  source  = "./ecs_alb"
  name    = "monitoring"
  subnets = ["${module.vpc_monitoring.subnets}"]

  loadbalancer_security_groups = [
    "${module.monitoring_cluster_asg.loadbalancer_sg_https_id}",
    "${module.monitoring_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "monitoring.wellcomecollection.org"
  vpc_id             = "${module.vpc_monitoring.vpc_id}"

  server_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
  client_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
}

module "api_alb" {
  source  = "./ecs_alb"
  name    = "api"
  subnets = ["${module.vpc_api.subnets}"]

  loadbalancer_security_groups = [
    "${module.api_cluster_asg.loadbalancer_sg_https_id}",
    "${module.api_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${module.vpc_api.vpc_id}"

  client_error_alarm_topic_arn = "${module.alb_client_error_alarm.arn}"
  server_error_alarm_topic_arn = "${module.alb_server_error_alarm.arn}"
}
