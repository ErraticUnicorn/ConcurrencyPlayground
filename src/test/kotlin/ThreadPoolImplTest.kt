import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

class ThreadPoolImplTest {

    @Test
    fun `assigns a task`() {
        println("Starting test")
        val boolean = AtomicBoolean(false)
        val threads = 2
        val threadPool = ThreadPoolImpl.create(threads)
        threadPool.submit { boolean.set(true) }
        threadPool.submit { println("Hey, it's me another job!") }
        Thread.sleep(1000)
        assertEquals(true, boolean.get())
    }

    @Test
    fun `gatherUnordered test`() {
        val tasks = listOf(FutureTask { println("Task one") }, FutureTask { println("Task two") })
//        tasks.gatherUnordered()
    }
}
