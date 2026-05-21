public class ThreadBasics {
    public static void main(String[] args) {
        // Creating a thread by extending the Thread class
        Thread thread1 = new Thread(() -> {
            System.out.println("Thread 1 is running: " + Thread.currentThread().getName());
        });

        // Creating a thread using Runnable interface
        Runnable task = () -> {
            System.out.println("Thread 2 is running: " + Thread.currentThread().getName());
        };
        Thread thread2 = new Thread(task);

        thread1.start();
        thread2.start();
    }
}
