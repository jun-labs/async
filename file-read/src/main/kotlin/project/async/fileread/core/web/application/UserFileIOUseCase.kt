package project.async.fileread.core.web.application

interface UserFileIOUseCase {
    fun asyncFileIO()

    fun sequentialFileIO()
}
