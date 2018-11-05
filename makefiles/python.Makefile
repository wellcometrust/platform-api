# Publish a ZIP file containing a Lambda definition to S3.
#
# Args:
#   $1 - Path to the Lambda src directory, relative to the root of the repo.
#
define publish_lambda
	$(ROOT)/docker_run.py --aws --root -- \
		$(DOCKER_IMG_PUBLISH_LAMBDA) \
		"$(1)" --key="lambdas/$(1).zip" --bucket="$(WELLCOME_INFRA_BUCKET)" --sns-topic="$(LAMBDA_PUSHES_TOPIC_ARN)"
endef


# Test a Python project.
#
# Args:
#   $1 - Path to the Python project's directory, relative to the root
#        of the repo.
#
define test_python
	$(ROOT)/docker_run.py --aws --dind -- \
		$(DOCKER_IMG_BUILD_TEST_PYTHON) $(1)

	$(ROOT)/docker_run.py --aws --dind -- \
		--net=host \
		--volume $(ROOT)/shared_conftest.py:/conftest.py \
		--workdir $(ROOT)/$(1) --tty \
		wellcome/test_python_$(shell basename $(1)):latest
endef


# Define a series of Make tasks (test, publish) for a Python Lambda.
#
# Args:
#	$1 - Name of the target.
#	$2 - Path to the Lambda source directory.
#
define lambda_target_template
$(1)-test:
	$(call test_python,$(2))

$(1)-publish:
	$(call publish_lambda,$(2))

$(ROOT)/$(2)/requirements.txt: $(ROOT)/$(2)/requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools

$(ROOT)/$(2)/test_requirements.txt: $(ROOT)/$(2)/test_requirements.in
	$(ROOT)/docker_run.py -- \
		--volume $(ROOT)/$(2)/src:/src micktwomey/pip-tools \
		pip-compile test_requirements.in
endef


# Define a series of Make tasks (build, test, publish) for an ECS service
# written in Python.
#
# Args:
#	$1 - Name of the ECS service.
#	$2 - Path to the associated Dockerfile.
#
define python_ecs_target_template
$(1)-build:
	$(call build_image,$(1),$(2))

$(1)-test:
	$(call test_python,$(STACK_ROOT)/$(1))

$(1)-publish: $(1)-build
	$(call publish_service,$(1))
endef
