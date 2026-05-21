public class ThreadSynchronization {
    private int count = 0;

    // Synchronized method to ensure thread safety
    public synchronized void increment() {
        count++;
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadSynchronization ts = new ThreadSynchronization();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) ts.increment();
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) ts.increment();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Final Count (should be 2000): " + ts.count);
    }
}
