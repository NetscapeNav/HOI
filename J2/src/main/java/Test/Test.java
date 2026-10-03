package Test;

import CustomList.CustomList;
import MyArrayList.MyArrayList;
import MySorter.MySorter;
import Node.Node;
import Sorter.Sorter;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 4;
        long insideDelay = 100;
        long betweenDelay = 100;
        String mode = "custom";
        if (args.length > 0) {
            workerCount = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            insideDelay = Long.parseLong(args[1]);
        }
        if (args.length > 2) {
            betweenDelay = Long.parseLong(args[2]);
        }
        if (args.length > 3) {
            mode = args[3];
        }
        if (workerCount < 1 || insideDelay < 0 || betweenDelay < 0) {
            System.out.println("Потоков должно быть не меньше 1, задержки неотрицательные");
            return;
        }
        if (!mode.equals("custom") && !mode.equals("array")) {
            System.out.println("Вариант списка: custom или array");
            return;
        }

        CustomList customList = new CustomList();
        MyArrayList arrayList = new MyArrayList();
        for (int i = 0; i < 100; i++) {
            String value = String.valueOf(i);
            if (mode.equals("custom")) {
                customList.addFirst(new Node(value, null));
            } else {
                arrayList.addFirst(value);
            }
        }

        Runnable sorter;
        if (mode.equals("custom")) {
            sorter = new Sorter(customList, insideDelay, betweenDelay);
        } else {
            sorter = new MySorter(arrayList, insideDelay, betweenDelay);
        }
        Thread[] workers = new Thread[workerCount];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(sorter);
        }

        long measuredSteps;
        double seconds;
        long started = System.nanoTime();
        try {
            for (Thread worker : workers) {
                worker.start();
            }
            Thread.sleep(30_000);
            if (mode.equals("custom")) {
                measuredSteps = customList.getSteps();
            } else {
                measuredSteps = arrayList.getSteps();
            }
            seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        } finally {
            for (Thread worker : workers) {
                worker.interrupt();
            }
            for (Thread worker : workers) {
                worker.join();
            }
        }

        System.out.println("Вариант: " + mode);
        System.out.println("Потоков: " + workerCount);
        System.out.println("Шагов: " + measuredSteps);
        System.out.println("Секунд: " + seconds);
        System.out.println("Шагов в секунду: " + measuredSteps / seconds);
    }
}
