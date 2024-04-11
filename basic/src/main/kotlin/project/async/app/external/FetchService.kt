package project.async.app.external

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

class FetchService {

    private val log = LoggerFactory.getLogger(FetchService::class.java)
    private val restTemplate = RestTemplate()
    private val webClient = WebClient.create()

    fun fetchSimpleHtmlPage(): String? {
        log.info("------------------------------------------------------------x>")
        return restTemplate.getForObject(SIMPLE_HTML_URL, String::class.java)
    }

    fun fetchThymeLeafPage(): String? {
        log.info("------------------------------------------------------------x>")
        return restTemplate.getForObject(THYMELEAF_URL, String::class.java)
    }

    suspend fun fetchSimpleHtmlPageWithSuspend(): String? {
        log.info("------------------------------------------------------------x>")
        return withContext(Dispatchers.IO) {
            restTemplate.getForObject(SIMPLE_HTML_URL, String::class.java)
        }
    }

    suspend fun fetchThymeLeafPageWithSuspend(): String? {
        log.info("------------------------------------------------------------x>")
        return webClient.get()
            .uri(THYMELEAF_URL)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }

    fun throwException() {
        throw RuntimeException("Exception")
    }

    companion object {
        private const val SIMPLE_HTML_URL = "http://help.websiteos.com/websiteos/example_of_a_simple_html_page.htm"
        private const val THYMELEAF_URL = "https://www.thymeleaf.org/documentation.html"
    }
}
