package project.async.fileread.core.domain.user

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class UserRepository {

    fun save(name: String): User {
        val id = idGenerator.incrementAndGet()
        val newUser = User(id, name)
        factory[id] = newUser
        return newUser
    }

    fun size(): Int {
        return factory.size
    }

    fun clear() {
        factory.clear()
    }

    companion object {
        private val idGenerator = AtomicLong(0L)
        private val factory: MutableMap<Long, User> = ConcurrentHashMap()
    }
}
