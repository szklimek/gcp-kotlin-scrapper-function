package functions

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.events.cloud.pubsub.v1.Message
import java.util.logging.Logger


class WebsiteArchiver: BackgroundFunction<Message> {
    override fun accept(payload: Message?, context: Context?) {
        val logger = Logger.getLogger(WebsiteScrapper::class.java.name)
        logger.info("Hello world!")
    }
}
