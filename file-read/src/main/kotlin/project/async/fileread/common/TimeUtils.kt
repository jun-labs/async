package project.async.fileread.common

class TimeUtils private constructor() {

    init {
        throw AssertionError("올바른 방식으로 생성자를 호출해주세요.")
    }

    companion object {
        fun getExecutionSeconds(endTime: Long, startTime: Long): Double {
            return (endTime - startTime) / 1000.0
        }
    }
}
