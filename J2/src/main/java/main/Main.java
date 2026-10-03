package main;

import CustomList.CustomList;
import MyArrayList.MyArrayList;
import MySorter.MySorter;
import Node.Node;
import Sorter.Sorter;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 1;
        long insideDelay = 1000;
        long betweenDelay = 1000;
        String mode = "array";
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
        Runnable sorter;
        Iterable<String> values;
        if (mode.equals("custom")) {
            sorter = new Sorter(customList, insideDelay, betweenDelay);
            values = customList;
        } else {
            sorter = new MySorter(arrayList, insideDelay, betweenDelay);
            values = arrayList;
        }

        Thread[] workers = new Thread[workerCount];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(sorter);
        }

        try {
            for (Thread worker : workers) {
                worker.start();
            }
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) {
                    for (String value : values) {
                        System.out.println(value);
                    }
                } else if (mode.equals("custom")) {
                    customList.addFirst(new Node(line, null));
                } else {
                    arrayList.addFirst(line);
                }
            }
        } finally {
            for (Thread worker : workers) {
                worker.interrupt();
            }
            for (Thread worker : workers) {
                worker.join();
            }
        }
    }
}
