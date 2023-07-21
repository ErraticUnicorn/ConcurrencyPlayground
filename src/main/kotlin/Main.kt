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
        fun create(numberOfThreads: Int): ThreadPool = TODO()
    }
}

class ThreadPoolImpl(private val numberOfThreads: Int) : ThreadPool {
    private val taskList: MutableList<FutureTask<*>> = mutableListOf(FutureTask { println("I am a job") })
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
                val task: FutureTask<*> = synchronized(taskList) {
                    if (taskList.isEmpty()) {
                        return@synchronized null
                    }
                    taskList.removeAt(0)
                } ?: continue

                try {
                    val result = task.callable()
                    task.setResult(result)
                } catch (e: Throwable) {
                    task.setError(e)
                }

                synchronized(task) {
                    task.setComplete(true)
                }
            }
        }
    }

    private inner class FutureTask<T>(val callable: () -> T) : Task<T> {
        private var result: T? = null
        private var error: Throwable? = null
        private var isDone: AtomicBoolean = AtomicBoolean(false)
        private val callbacks: MutableList<(result: T?, error: Throwable?) -> Unit> = mutableListOf()

        override fun whenComplete(callback: (result: T?, error: Throwable?) -> Unit) {
            if (isDone.get()) {
                callback(result, error)
            } else {
                callbacks.add(callback)
            }
        }

        fun setResult(result: Any?) {
            this.result = result as T?
        }

        fun setError(throwable: Throwable?) {
            error = throwable
        }

        fun setComplete(complete: Boolean) {
            isDone.set(complete)
            if (complete) {
                callbacks.forEach() {
                    it(result, error)
                }
                callbacks.clear()
            }
        }
    }

    companion object {
        fun create(numberOfThreads: Int): ThreadPool = ThreadPoolImpl(numberOfThreads)
    }
}
