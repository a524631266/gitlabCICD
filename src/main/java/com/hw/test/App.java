package com.hw.test;

import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello World!");
        Thread t1 = new Thread(() -> {
            while (true) {
                System.out.println("version22");

                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
        t1.setDaemon(true);
        t1.start();
        t1.join();
    }
}
