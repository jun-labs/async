package project.async.fileread.core.web.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import project.async.fileread.core.web.application.UserFileIOUseCase

@RestController
@RequestMapping("/api/file-io")
class UserAPI(
    private val userFileIOUseCase: UserFileIOUseCase
) {

    @GetMapping("/async")
    fun asyncFileIO() {
        userFileIOUseCase.asyncFileIO()
    }

    @GetMapping("/sequential")
    fun syncFileIO() {
        userFileIOUseCase.sequentialFileIO()
    }
}
