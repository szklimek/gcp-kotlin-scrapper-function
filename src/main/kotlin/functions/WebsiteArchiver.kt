package functions

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import com.google.events.cloud.pubsub.v1.Message
import it.skrape.core.document
import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.logging.Logger

/**
 * Function that loads websites and stores their html code in Cloud Storage
 *
 * Requires providing following environment variables:
 * `GCP_PROJECT` - id of Google Cloud Platform project
 * `BUCKET_NAME` - name of Cloud Storage bucket
 * `WEBSITES_CONFIG_JSON` - list of websites ([Website]) encoded as JSON, each containing "name" and "url"
 *      e.g. [{"name":"site1","url":"https://site1.com"},{"name":"site2","url":"https://site2.com"}]
 */
class WebsiteArchiver : BackgroundFunction<Message> {
    companion object {
        const val MAX_PAGE_SIZE = 10000000 /* ~10MB */
        const val ENV_GCP_PROJECT = "GCP_PROJECT"
        const val ENV_BUCKET_NAME = "BUCKET_NAME"
        const val ENV_WEBSITES_CONFIG_JSON = "WEBSITES_CONFIG_JSON"
    }

    override fun accept(payload: Message?, context: Context?) {
        val logger = Logger.getLogger(WebsiteArchiver::class.java.name)
        runBlocking {
            logger.info("Function started...")
            val websites = Json.decodeFromString<List<Website>>(System.getenv(ENV_WEBSITES_CONFIG_JSON))
            val projectId = System.getenv(ENV_GCP_PROJECT)
            val bucketName = System.getenv(ENV_BUCKET_NAME)

            val storage = StorageOptions.newBuilder().setProjectId(projectId).build().service
            websites.forEach {
                logger.info("Fetching $it")
                val response = skrape(BrowserFetcher) {
                    request { url = it.url }
                    response {
                        logger.info("Response status: $responseStatus, headers: $headers")
                        document.html
                    }
                }
                val pageSize = response.length
                logger.info("Downloaded page size: ${response.length}")
                if (pageSize > MAX_PAGE_SIZE) error("Page is too big ($pageSize). Aborting")

                val filePath = "${it.name}/${LocalDate.now()}.html"

                logger.info("Uploading html to Cloud Storage. Path: $filePath")
                storage.createFrom(
                    BlobInfo.newBuilder(bucketName, filePath).build(),
                    response.byteInputStream()
                )
                logger.info("Website snapshot uploaded: $filePath")
            }
        }
    }

    @Serializable
    data class Website(val name: String, val url: String)
}
