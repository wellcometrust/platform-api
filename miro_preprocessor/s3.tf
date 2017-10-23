resource "aws_s3_bucket" "wellcomecollection-images" {
  bucket = "wellcomecollection-images"
  acl    = "private"

  # JP2 master assets (same set as catalogue API)
  lifecycle_rule {
    id     = "library"
    prefix = "library/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  # JP2 master assets
  lifecycle_rule {
    id     = "cold_store"
    prefix = "cold_store/"

    transition {
      days          = 30
      storage_class = "GLACIER"
    }

    enabled = true
  }

  # JP2 master assets
  lifecycle_rule {
    id     = "tandem_vault"
    prefix = "tandem_vault/"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    enabled = true
  }

  lifecycle {
    prevent_destroy = true
  }
}
