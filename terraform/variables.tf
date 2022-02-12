variable "project" {}
variable "websites_config_json" {}
variable "region" {
  default = "europe-west1"
}
variable "function_schedule" {
  # Every day at 20:00
  default = "0 20 * * *"
}
variable "function_schedule_timezone" {
  default = "Europe/Berlin"
}
