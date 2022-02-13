# Website Archiver Cloud Function written in Kotlin

Simple Google Cloud Platform function (https://cloud.google.com/functions) for website archiving written in Kotlin.

It runs on regular schedule and uploads downloaded html page to Cloud Storage.

Resources used:

- Cloud Functions
- Cloud Storage
- Pub/Sub
- Cloud Scheduler

# Deployment

Project contains Terraform definitions for all required resources (see [terraform directory](terraform)).

**IMPORTANT**

Cloud resources used in the project are charged based on the usage. Make sure to calculate the cost of running to avoid
unexpected spending. See [Google Cloud Pricing Calculator](https://cloud.google.com/products/calculator)

## Requirements

- Google Cloud Platform account with a project connected with billing account
- `gcloud` CLI (https://cloud.google.com/sdk/docs/install)
- `terraform` CLI installed

Helpful links:

- [Create, modify, or close your self-serve Cloud Billing account](https://cloud.google.com/billing/docs/how-to/manage-billing-account)
- [Installing the gcloud CLI](https://cloud.google.com/sdk/docs/install)
- [Download Terraform](https://www.terraform.io/downloads)
- Introductory
  tutorial: [Get started with Terraform and Google Cloud](https://learn.hashicorp.com/collections/terraform/gcp-get-started)
- Great tutorial from Ruan
  Martinelli: [Deploying to Cloud Functions with Terraform](https://ruanmartinelli.com/posts/terraform-cloud-functions-nodejs-api)

## Running locally

In order to test the function locally run (from project root dir):

```shell
./gradlew clean build  runFunction -Prun.functionTarget=functions.WebsiteArchiver
```

Simulate sending Pub/Sub message to execute function by sending POST request:

```shell
curl localhost:8080 \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{
        "context": {
          "eventId":"1144231683168617",
          "timestamp":"2020-05-06T07:33:34.556Z",
          "eventType":"google.pubsub.topic.publish",
          "resource":{
            "service":"pubsub.googleapis.com",
            "name":"projects/sample-project/topics/gcf-test",
            "type":"type.googleapis.com/google.pubsub.v1.PubsubMessage"
          }
        },
        "data": {
          "@type": "type.googleapis.com/google.pubsub.v1.PubsubMessage",
          "attributes": {
             "attr1":"attr1-value"
          },
          "data": "d29ybGQ="
        }
      }'
```

## Deploying to Google Cloud Platform from local machine

Init and apply Terraform configuration:

```shell
# From root project directory
cd terraform
terraform init
terraform plan
terraform apply
```

Provide required parameters:

- `project`: project id on Google Cloud Platforn
- `websites_config_json`: list of websites to download formatted in JSON, where each site requires `name` and `url`
  parameters e.g.: `[{"name":"site1","url":"https://site1.com"},{"name":"site2","url":"https://site2.com"}]`

There are also additional parameters that can be adjusted by using environment variables e.g.:

```shell
# Setting Cloud Scheduler to send message every minute (see more: https://cloud.google.com/scheduler/docs/configuring/cron-job-schedules#defining_the_job_schedule)
export TF_VAR_function_schedule='* * * * *'

# Setting timezone for Cloud Scheduler
export TF_VAR_function_schedule_timezone='Europe/Berlin'

# Setting region for all resources
export TF_VAR_region='europe-west1'

terraform apply
```

For further improvements simply re-apply configuration:

```shell
terraform plan
terraform apply
```

In case the function is not needed anymore all resources can be deleted by:

```shell
terraform destroy
```

Double-check if all resources were deleted and clean them manually from GCP console if something left to stop charges.
