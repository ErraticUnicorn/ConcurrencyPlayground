import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}

interface Task<T> {
    fun whenComplete(callback: (result: T?, error: Throwable?) -> Unit)
}

interface ThreadPool {
    fun <T> submit(task: () -> T): Task<T>

    companion object {
        fun create(numberOfThreads: Int): ThreadPool = ThreadPoolImpl.create(numberOfThreads)
    }
}

class ThreadPoolImpl(private val numberOfThreads: Int) : ThreadPool {
    private val taskList: BlockingQueue<FutureTask<*>> = ArrayBlockingQueue(MAX_CAPACITY)
    private val threads: List<TaskThread> = List(numberOfThreads) { TaskThread() }

    init {
        threads.forEach { it.start() }
    }

    override fun <T> submit(task: () -> T): Task<T> {
        val future = FutureTask(task)
        synchronized(taskList) {
            taskList.add(future)
        }
        return future
    }

    private inner class TaskThread : Thread() {
        override fun run() {
            while (true) {
                val task: FutureTask<*> = taskList.take()

                try {
                    val result = task.callable()
                    task.setResult(result)
                } catch (e: Throwable) {
                    task.setError(e)
                }
            }
        }
    }

    companion object {

        private const val MAX_CAPACITY = 100000
        fun create(numberOfThreads: Int): ThreadPool = ThreadPoolImpl(numberOfThreads)
    }
}

class FutureTask<T>(val callable: () -> T) : Task<T> {
    private var result: T? = null
    private var error: Throwable? = null
    private var isDone: AtomicBoolean = AtomicBoolean(false)
    private val callbacks: MutableList<(result: T?, error: Throwable?) -> Unit> = mutableListOf()

    override fun whenComplete(callback: (result: T?, error: Throwable?) -> Unit) {
        synchronized(this) {
            if (isDone.get()) {
                callback(result, error)
            } else {
                // race condition
                callbacks.add(callback)
            }
        }
    }

    fun setResult(result: Any?) {
        synchronized(this) {
            this.result = result as T?
            setComplete(true)
            callbacks.forEach() {
                it(result, error)
            }
            callbacks.clear()
        }
    }

    fun setError(throwable: Throwable?) {
        synchronized(this) {
            error = throwable
            setComplete(true)
        }
    }

    private fun setComplete(complete: Boolean) {
        isDone.set(complete)
    }
}

fun <T> List<Task<T>>.gatherUnordered(): Task<MutableList<T>> {
    val results = mutableListOf<T>()
    val futureTasks = FutureTask { results }

    this.forEach { task ->
        task.whenComplete { result, error ->
            if (error != null) {
                futureTasks.setError(error)
            } else if (result != null) {
                futureTasks.setResult(result)
                results.add(result)
            }
        }
    }
    // TODO: Do not use mutable list
    return futureTasks
}
