provider "google" {
  project = var.project
  region  = var.region
}

module "website-archiver" {
  source                     = "./modules/function"
  project                    = var.project
  websites_config_json       = var.websites_config_json
  function_name              = "website-archiver"
  function_entry_point       = "functions.WebsiteArchiver"
  function_schedule          = var.function_schedule
  function_schedule_timezone = var.function_schedule_timezone
  function_execution_timeout = var.function_execution_timeout
}
