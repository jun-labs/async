package project.async.fileread.core.web.application.service

import org.springframework.stereotype.Service
import project.async.fileread.common.FileUtils
import project.async.fileread.common.TimeUtils.Companion.getExecutionSeconds
import project.async.fileread.core.domain.user.UserRepository
import project.async.fileread.core.web.application.UserFileIOUseCase
import java.lang.System.currentTimeMillis
import java.util.Collections.synchronizedList
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Service
class UserService(
    private val userRepository: UserRepository
) : UserFileIOUseCase {

    private val data: List<String> = FileUtils.extractNames()

    private val file = FileUtils.getFile()

    override fun asyncFileIO() {
        asyncIO()
    }

    override fun sequentialFileIO() {
        sequentialIO()
    }

    private fun asyncIO() {
        val result = synchronizedList(mutableListOf<String>())
        val startTime = currentTimeMillis()
        val lines = data

        val executor = Executors.newFixedThreadPool(16)
        val futures = mutableListOf<CompletableFuture<Void>>()
        val count = AtomicInteger()

        val linesPerThread = 84000

        for (index in 0 until 16) {
            val startLine = index * linesPerThread
            val endLine = minOf((index + 1) * linesPerThread, lines.size)

            val future = CompletableFuture.runAsync({
                for (subIndex in startLine until endLine) {
                    count.incrementAndGet()
                    val name = lines[subIndex]
                    result.add(name)
                    userRepository.save(name)
                }
            }, executor)
            futures.add(future)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executor.shutdown()

        val seconds = getExecutionSeconds(currentTimeMillis(), startTime)
        print(count, result, seconds)
        userRepository.clear()
    }

    private fun sequentialIO() {
        val result: MutableList<String> = mutableListOf()
        val startTime = currentTimeMillis()

        var count = 0
        file.forEachLine {
            val name = it
            result.add(name)
            userRepository.save(name)
            count++
        }

        val seconds = getExecutionSeconds(currentTimeMillis(), startTime)
        print(count, result, seconds)
        userRepository.clear()
    }

    private fun print(
        count: AtomicInteger,
        result: MutableList<String>,
        seconds: Double
    ) {
        println("Count = ${count.get()}")
        println("TaskSize: ${result.size} 개")
        println("UserSize: ${userRepository.size()} 개")
        println("Time: $seconds s")
    }

    private fun print(
        count: Int,
        result: MutableList<String>,
        durationSeconds: Double
    ) {
        println("Count: $count")
        println("TaskSize: ${result.size} 개")
        println("UserSize: ${userRepository.size()} 개")
        println("Time: $durationSeconds s")
    }
}
