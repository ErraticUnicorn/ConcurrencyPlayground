import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.atomic.AtomicBoolean

class ThreadPoolImplTest {

    @Test
    fun `assigns a task`() {
        println("Starting test")
        var boolean = AtomicBoolean(false)
        val threads = 2
        val threadPool = ThreadPoolImpl.create(threads)
        threadPool.submit { boolean = AtomicBoolean(true) }
        Thread.sleep(10000)
        assertEquals(true, boolean)
    }
}
