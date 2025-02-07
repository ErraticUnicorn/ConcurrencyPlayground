import unittest
import threading
from time import sleep

def print_message():
    sleep(1)
    print("Thread ran!")

class Test_TestThreads(unittest.TestCase):
    def simple_test():
        assert(3 == 4)
        
    def test_thread_execution(self):
        t = threading.Thread(target=print_message)
        t.start()
        t.join()
        self.assertTrue(t.is_alive() == False) 

if __name__ == '__main__':
    unittest.main()