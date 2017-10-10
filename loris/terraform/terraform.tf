terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "platform-loris.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform-lambda"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_api" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform.tfstate"
    region = "eu-west-1"
  }
}

