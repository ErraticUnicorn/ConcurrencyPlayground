import org.junit.Assert.assertNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ThreadPoolImplTestDirectorsCut {

    @Test
    fun `submit executes the task in single thread`() {
        val resultHolder = AtomicReference<Int?>()
        val errHolder = AtomicReference<Throwable?>()
        val whenCompleteCounter = AtomicInteger(0)

        val unit = ThreadPool.create(1)
        val latch = CountDownLatch(2)
        val task = unit.submit {
            latch.countDown()
            11
        }
        task.whenComplete { result, error ->
            resultHolder.set(result)
            errHolder.set(error)
            whenCompleteCounter.incrementAndGet()
            latch.countDown()
        }
        val res = latch.await(1, TimeUnit.SECONDS)
        assertTrue(res)
        assertEquals(11, resultHolder.get())
        assertNull(errHolder.get())
        assertEquals(1, whenCompleteCounter.get())
    }

    @Test
    fun `submit executes tasks in multiple threads`() {
        val unit = ThreadPool.create(2)
        val latch = CountDownLatch(1)
        val task1Completed = AtomicBoolean(false)
        val task2Completed = AtomicBoolean(false)
        val task3Completed = AtomicBoolean(false)

        unit.submit {
            latch.await()
            task1Completed.set(true)
            "blah"
        }
        unit.submit {
            latch.await()
            task2Completed.set(true)
            111
        }
        unit.submit {
            task3Completed.set(true)
        }

        Thread.sleep(100)
        assertFalse(task1Completed.get())
        assertFalse(task2Completed.get())
        assertFalse(task3Completed.get())

        latch.countDown()
        Thread.sleep(100)
        assertTrue(task1Completed.get())
        assertTrue(task2Completed.get())
        assertTrue(task3Completed.get())
    }

    @Test
    fun `whenComplete is called when set before task completion`() {
        val unit = ThreadPool.create(2)
        val latch = CountDownLatch(1)
        val whenCompleteResult1 = AtomicReference<Result<String?>>()
        val whenCompleteResult2 = AtomicReference<Result<String?>>()

        val task1 = unit.submit {
            latch.await()
            "blah"
        }
        val task2 = unit.submit {
            latch.await()
            throw MyThrowable("test")
        }

        task1.whenComplete { result, error ->
            val value = if (error != null) Result.failure(error) else Result.success(result)
            whenCompleteResult1.set(value)
        }
        task2.whenComplete { result, error ->
            val value = if (error != null) Result.failure(error) else Result.success(result)
            whenCompleteResult2.set(value)
        }
        latch.countDown()

        Thread.sleep(100)
        assertEquals(Result.success("blah"), whenCompleteResult1.get())
        val error = whenCompleteResult2.get().exceptionOrNull()
        assertTrue(error is MyThrowable)
        assertEquals("test", error!!.message)
    }

    @Test
    fun `whenComplete is called when set after task completion`() {
        val unit = ThreadPool.create(2)
        val latch = CountDownLatch(2)
        val whenCompleteResult1 = AtomicReference<Result<String?>>()
        val whenCompleteResult2 = AtomicReference<Result<String?>>()

        val task1 = unit.submit {
            latch.countDown()
            "blah"
        }
        val task2 = unit.submit {
            latch.countDown()
            throw MyThrowable("test")
        }
        latch.await(1, TimeUnit.SECONDS)
        Thread.sleep(100)

        task1.whenComplete { result, error ->
            val value = if (error != null) Result.failure(error) else Result.success(result)
            whenCompleteResult1.set(value)
        }
        task2.whenComplete { result, error ->
            val value = if (error != null) Result.failure(error) else Result.success(result)
            whenCompleteResult2.set(value)
        }

        Thread.sleep(100)
        assertEquals(Result.success("blah"), whenCompleteResult1.get())
        val error = whenCompleteResult2.get().exceptionOrNull()
        assertTrue(error is MyThrowable)
        assertEquals("test", error!!.message)
    }

    @Test
    @Timeout(5, unit = TimeUnit.SECONDS)
    fun `submit many tasks`() {
        val executorCounter = AtomicInteger(0)
        val unit = ThreadPool.create(4)

        repeat(1000) {
            unit.submit {
                executorCounter.incrementAndGet()
            }
        }

        do {
            val current = executorCounter.get()
            Thread.sleep(10)
        } while (current < 1000)

        Thread.sleep(100)
        assertEquals(1000, executorCounter.get())
    }

    @Test
    fun `should survive exception in whenComplete`() {
        val unit = ThreadPool.create(1)
        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)

        val task1 = unit.submit {
            latch.await()
        }

        task1.whenComplete { result, error ->
            throw MyThrowable("test")
        }

        latch.countDown()
        Thread.sleep(100)

        unit.submit {
            executed.set(true)
        }

        Thread.sleep(100)
        assertTrue(executed.get())
    }
}


class MyThrowable(msg: String) : Throwable(msg)