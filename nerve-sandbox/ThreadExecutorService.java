import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadExecutorService {
    public static void main(String[] args) {
        // Creating a pool of 2 threads
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 1; i <= 5; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " is running by " + Thread.currentThread().getName());
            });
        }

        executor.shutdown();
        System.out.println("All tasks submitted.");
    }
}
