package util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author baofeng
 * @date 2023/06/06
 */
public class DdcTaskExecutor {
    private static int invokerPoolSize = 10;
    private static int loadFactor = 20;
    private static BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(invokerPoolSize * loadFactor);
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(invokerPoolSize, invokerPoolSize, 60L, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());

    public static void execute(Runnable task) {
        executor.execute(task);
    }

}
