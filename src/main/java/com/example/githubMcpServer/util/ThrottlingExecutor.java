package com.example.githubMcpServer.util;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
public class ThrottlingExecutor {

    private static final int MAX_RETRIES = 5;

    private static final long INITIAL_DELAY = 1000;

    public <T> T execute(Supplier<T> operation) {

        long delay = INITIAL_DELAY;

        for (int attempt = 1;
             attempt <= MAX_RETRIES;
             attempt++) {

            try {

                return operation.get();

            } catch (HttpStatusCodeException ex) {

                HttpStatusCode statusCode =
                        ex.getStatusCode();

                if (statusCode.value() != 429) {
                    throw ex;
                }

                System.out.println(
                        "[THROTTLE] 429 received. Retry "
                                + attempt);

            } catch (Exception ex) {

                System.out.println(
                        "[RETRY] Attempt "
                                + attempt);

                if (attempt == MAX_RETRIES) {
                    throw ex;
                }
            }

            exponentialBackoff(delay);

            delay *= 2;
        }

        throw new RuntimeException(
                "Retry limit exceeded");
    }

    private void exponentialBackoff(long delay) {

        try {

            long jitter =
                    ThreadLocalRandom.current()
                            .nextLong(500);

            long waitTime =
                    delay + jitter;

            System.out.println(
                    "[WAIT] "
                            + waitTime
                            + " ms");

            Thread.sleep(waitTime);

        } catch (InterruptedException ex) {

            Thread.currentThread()
                    .interrupt();

            throw new RuntimeException(ex);
        }
    }
}