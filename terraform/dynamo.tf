resource "aws_dynamodb_table" "calm-dynamodb-table" {
  name             = "CalmData"
  read_capacity    = 5
  write_capacity   = 5
  hash_key         = "RecordID"
  range_key        = "RecordType"
  stream_enabled   = true
  stream_view_type = "NEW_IMAGE"

  attribute {
    name = "RecordID"
    type = "S"
  }

  attribute {
    name = "RecordType"
    type = "S"
  }

  attribute {
    name = "RefNo"
    type = "S"
  }

  attribute {
    name = "AltRefNo"
    type = "S"
  }

  global_secondary_index = {
    name            = "RefNo"
    hash_key        = "RefNo"
    read_capacity   = 5
    write_capacity  = 5
    projection_type = "ALL"
  }

  global_secondary_index = {
    name            = "AltRefNo"
    hash_key        = "AltRefNo"
    read_capacity   = 5
    write_capacity  = 5
    projection_type = "ALL"
  }
}

resource "aws_dynamodb_table" "identifiers" {
  name           = "Identifiers"
  read_capacity  = 5
  write_capacity = 5
  hash_key       = "CanonicalID"

  attribute {
    name = "CanonicalID"
    type = "S"
  }

  attribute {
    name = "MiroID"
    type = "S"
  }

  global_secondary_index = {
    name            = "MiroID"
    hash_key        = "MiroID"
    read_capacity   = 5
    write_capacity  = 5
    projection_type = "ALL"
  }
}
