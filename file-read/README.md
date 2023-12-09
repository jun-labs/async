# File Read

When comparing the performance of processing file data, let's contrast the approach of reading files `sequentially`, one by one, with the method of processing them by dividing the workload `asynchronously`.

<br/>

In more detail, it involves reading approximately 1.34 million lines of text. The comparison is between processing these lines sequentially, one line at a time, and dividing the task across 16 threads for concurrent processing. The data used `simple text` and the database used `application in-memory`.

> For the convenience of the test, clearing the cache of the data once read was not considered. To be more precise, the cache should be cleared for each test, and all conditions should be kept identical.

<br/><br/><br/><br/><br/><br/>

First, sequential I/O operations. Read each line one by one to handle the operations.

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) : UserFileIOUseCase {

    private val file = FileUtils.getFile()

    override fun sequentialFileIO() {
        sequentialIO()
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

    ......

}
```

<br/><br/><br/><br/><br/><br/>

Next is asynchronous I/O operations, which divide large operations into several and asynchronize them at the same time.

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) : UserFileIOUseCase {

    private val data: List<String> = FileUtils.extractNames()

    override fun asyncFileIO() {
        asyncIO()
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

    ......

}
```

<br/><br/><br/><br/><br/><br/>

After each task, all data was deleted.

````kotlin
userRepository.clear()
````

<br/><br/><br/><br/><br/><br/>

## Getting Started

> You should install jdk 17 or higher. <br/>

<br/><br/><br/>

## Run Application

````text
$ ./gradlew bootRun
````

<br/><br/>

## Run Test

````text
$ ./gradlew test
````

<br/><br/>

## Run Build

````text
$ ./gradlew build
````

<br/><br/><br/>

## Result

The results may vary depending on the environment of the user you are experimenting with.

```shell
# Sequential
Count: 1344000
ResultSize: 1344000 개
Time: 0.221 s

# Async
Count = 1344000
ResultSize: 1344000 개
Time: 0.62 s
```

<br/><br/><br/>

## Env
&nbsp;&nbsp; - Java 17 <br/>
&nbsp;&nbsp; - SpringBoot 3.0 <br/>

<br/>
