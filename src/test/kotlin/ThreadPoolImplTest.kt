import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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
        val threadPool = ThreadPoolImpl.create(2)
        val taskOne = threadPool.submit { Thread.sleep(1000); "Task one" }
        val taskTwo = threadPool.submit { "Task two" }
        val tasks = listOf(taskOne, taskTwo)
        val task: Task<List<String>> = tasks.gatherUnordered()
        val r = AtomicReference<List<String>>()
        task.whenComplete { result, error ->
            println("I am going to put a result! $result, oh shit there's an error: $error")
            r.set(result)
        }

        while (r.get() == null) {
            Thread.sleep(10)
        }

        assertEquals(listOf("Task one", "Task two"), r.get().sorted())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `gatherUnordered test thicc`() {
        val threadPool = ThreadPoolImpl.create(200)
        val tasks = (0..<10000).map {
            threadPool.submit { Thread.sleep(1); "Task $it" }
        }
        val task: Task<List<String>> = tasks.gatherUnordered()
        val r = AtomicReference<List<String>>()
        task.whenComplete { result, error ->
            println("I am going to put a result! $result, oh shit there's an error: $error")
            r.set(result)
        }

        while (r.get() == null) {
            Thread.sleep(10)
        }

        assertEquals(10000, r.get().size)
    }
}
