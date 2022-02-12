locals {
  timestamp = formatdate("YYMMDDhhmmss", timestamp())
  root_dir  = abspath("../")
}

# Compress source code
data "archive_file" "source" {
  type        = "zip"
  source_dir  = local.root_dir
  output_path = "/tmp/function-${local.timestamp}.zip"
}

# Create bucket that will host the source code
resource "google_storage_bucket" "bucket" {
  name          = "${var.project}-function"
  location      = "EU"
  force_destroy = true
}

# Add source code zip to bucket
resource "google_storage_bucket_object" "zip" {
  # Append file MD5 to force bucket to be recreated
  name   = "source.zip#${data.archive_file.source.output_md5}"
  bucket = google_storage_bucket.bucket.name
  source = data.archive_file.source.output_path
}

# Enable Cloud Functions API
resource "google_project_service" "cf" {
  project = var.project
  service = "cloudfunctions.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

# Enable Cloud Build API
resource "google_project_service" "cb" {
  project = var.project
  service = "cloudbuild.googleapis.com"

  disable_dependent_services = true
  disable_on_destroy         = false
}

# Create Pub Sub topic
resource "google_pubsub_topic" "topic" {
  name = "function-trigger"
}

# Create Cloud Scheduler
resource "google_cloud_scheduler_job" "job" {
  name        = "function-scheduler-job"
  description = "Cloud Scheduler that publishes message to topic regularly"
  schedule    = var.function_schedule
  time_zone   = var.function_schedule_timezone

  pubsub_target {
    topic_name = google_pubsub_topic.topic.id
    data       = base64encode("Non-empty string")
  }
}

# Create Cloud Function
resource "google_cloudfunctions_function" "function" {
  name    = var.function_name
  runtime = "java11"

  available_memory_mb   = 1024
  source_archive_bucket = google_storage_bucket.bucket.name
  source_archive_object = google_storage_bucket_object.zip.name
  entry_point           = var.function_entry_point
  max_instances         = 1

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = google_pubsub_topic.topic.id
  }

  environment_variables = {
    GCP_PROJECT          = var.project
    BUCKET_NAME          = google_storage_bucket.bucket.name
    WEBSITES_CONFIG_JSON = var.websites_config_json
  }
}
