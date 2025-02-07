import threading
from time import sleep

lock = threading.Lock()



def print_message():
    with lock:
        print("Lock acquired")
        sleep(5.0)
        print("We out here!!!") 

t1 = threading.Thread(target=print_message)
t2 = threading.Thread(target=print_message)

# start the threads
t1.start()
t2.start()


# wait for the threads to complete
t1.join()
t2.join()


lock1 = threading.Lock()
lock2 = threading.Lock()

def thread1():
    lock1.acquire()
    print("Thread 1 acquired lock1")

    sleep(1)  # Simulate some processing
    lock2.acquire()  # Deadlock if thread 2 already has lock2
    print("Thread 1 acquired lock2")

    lock2.release()
    lock1.release()

def thread2():
    lock2.acquire()
    print("Thread 2 acquired lock2")

    sleep(1)  # Simulate some processing
    lock1.acquire()  # Deadlock if thread 1 already has lock1
    print("Thread 2 acquired lock1")

    lock1.release()
    lock2.release()

t1 = threading.Thread(target=thread1)
t2 = threading.Thread(target=thread2)

t1.start()
t2.start()

t1.join()
t2.join()