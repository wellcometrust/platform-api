# We can run two versions of the API: a "production" and a "staging" API.
# The idea is that we can run the new version of the API behind a different
# hostname, then promote it to production when it's ready to go.
#
# This file contains all the different bits of config for the two versions,
# followed by heavy abuse of Terraform's ternary operator in services.tf.
#
# https://www.terraform.io/docs/configuration/interpolation.html#conditionals

variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "eu-west-1"
}

variable "az_count" {
  description = "Number of AZs to cover in a given AWS region"
  default     = "2"
}

variable "key_name" {
  description = "Name of AWS key pair"
}

variable "release_ids" {
  description = "Release tags for platform apps"
  type        = "map"
}

variable "api_prod_host" {
  description = "Hostname to use for the production API"
  default     = "api.wellcomecollection.org"
}

variable "api_stage_host" {
  description = "Hostname to use for the production API"
  default     = "api-stage.wellcomecollection.org"
}

variable "es_cluster_credentials" {
  description = "Credentials for the Elasticsearch cluster"
  type        = "map"
}

# These variables will change fairly regularly, whenever we want to swap the
# staging and production APIs.

variable "production_api" {
  description = "Which version of the API is production? (romulus | remus)"
  default     = "remus"
}

variable "pinned_romulus_api" {
  description = "Which version of the API image to pin romulus to, if any"
  default     = "cd3bd08cbf5ece8af17b2baad979e5c0e589b880"
}

variable "pinned_romulus_api_nginx" {
  description = "Which version of the nginx API image to pin romulus to, if any"
  default     = "4d0b58c7cd5feefbe77637f7fcda0d93b645e11b"
}

variable "pinned_remus_api" {
  description = "Which version of the API image to pin remus to, if any"
  default     = "58d71745bc3b50ef0bde45be7e27a63e1dee4b1a"
}

variable "pinned_remus_api_nginx" {
  description = "Which version of the nginx API image to pin remus to, if any"
  default     = "4d0b58c7cd5feefbe77637f7fcda0d93b645e11b"
}
