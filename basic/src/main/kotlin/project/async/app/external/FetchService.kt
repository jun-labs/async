package project.async.app.external

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

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
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }


    /**
     * 동기 처리 예제.
     * Webclient는 비동기를 지원하지만, 동기적으로 사용할 수도 있다.
     * 단, 권장하지 않는다.
     * */
    fun fetchThymeLeafPageWithSync(): String? {
        log.info("------------------------------------------------------------x>")
        return webClient.get()
            .uri(THYMELEAF_URL)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    fun fetchThymeLeafPageWithSyncV2(): String? {
        log.info("------------------------------------------------------------x>")
        return webClient.get()
            .uri(THYMELEAF_URL)
            .accept(MediaType.TEXT_HTML)
            .exchangeToMono { response ->
                response.bodyToMono(String::class.java)
            }
            .block()
    }

    fun fetchThymeLeafPageWithSyncV3(): String? {
        log.info("------------------------------------------------------------x>")
        return webClient.get()
            .uri(THYMELEAF_URL)
            .accept(MediaType.TEXT_HTML)
            .exchangeToMono { response ->
                when {
                    response.statusCode().is2xxSuccessful -> {
                        response.bodyToMono(String::class.java)
                    }

                    else -> {
                        response.releaseBody()
                        Mono.error(RuntimeException("Exception"))
                    }
                }
            }
            .block()
    }

    fun throwException() {
        throw RuntimeException("Exception")
    }

    companion object {
        private const val SIMPLE_HTML_URL = "http://help.websiteos.com/websiteos/example_of_a_simple_html_page.htm"
        private const val THYMELEAF_URL = "https://www.thymeleaf.org/documentation.html"
    }
}
