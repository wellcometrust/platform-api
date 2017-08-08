data "template_file" "job_definition" {
  template = "${file("${path.module}/register_batch_job.json.template")}"

  vars {
    name        = "${var.name}"
    image_uri   = "${var.image_uri}"
    memory      = "${var.memory}"
    vcpus       = "${var.vcpus}"
    jobRoleArn  = "${var.jobRoleArn}"
    environment = "[${join(",", var.job_vars}]"
  }
}


resource "null_resource" "export_rendered_template" {
  triggers {
    template = "${data.template_file.job_definition.rendered}"
  }

  provisioner "local-exec" {
    command = "cat > /app/batch_job_${var.name}.json <<EOL\n${data.template_file.job_definition.rendered}\nEOL"

    on_failure = "fail"
  }

  provisioner "local-exec" {
    command = "/app/provisioners/aws_batch_job.py create /app/batch_job_${var.name}.json"

    on_failure = "fail"
  }
}
