package project.async.fileread.common

import java.io.File

class FileUtils private constructor() {

    init {
        throw AssertionError("올바른 방식으로 생성자를 호출해주세요.")
    }

    companion object {
        fun getFile(): File {
            return File("src/main/resources/data/data.txt")
        }

        fun extractNames() = Thread.currentThread().contextClassLoader
            .getResourceAsStream("data/data.txt")?.bufferedReader()!!.readLines()
    }
}
