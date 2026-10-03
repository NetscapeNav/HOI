import MyArrayList.MyArrayList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MyArrayListTest {

    @Test
    public void emptyListTest() throws InterruptedException {
        MyArrayList list = new MyArrayList();
        list.sortPass(0, 0);
        assertFalse(list.iterator().hasNext());
        assertEquals(0, list.getSteps());
    }

    @Test
    public void singleElementTest() throws InterruptedException {
        MyArrayList list = new MyArrayList();
        list.addFirst("a");
        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a"), list);
        assertEquals(0, list.getSteps());
    }

    @Test
    public void sortPassTest() throws InterruptedException {
        MyArrayList list = new MyArrayList();
        list.addFirst("a");
        list.addFirst("b");
        list.addFirst("c");
        assertIterableEquals(java.util.List.of("c", "b", "a"), list);

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("b", "a", "c"), list);
        assertEquals(2, list.getSteps());
    }

    @Test
    public void duplicateValuesTest() throws InterruptedException {
        MyArrayList list = new MyArrayList();
        list.addFirst("a");
        list.addFirst("b");
        list.addFirst("a");

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a", "a", "b"), list);
        assertEquals(2, list.getSteps());

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a", "a", "b"), list);
        assertEquals(4, list.getSteps());
    }

    @Test
    public void concurrentSortTest() throws Exception {
        MyArrayList list = new MyArrayList();
        ArrayList<String> expected = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            list.addFirst(String.valueOf(i));
            expected.add(String.valueOf(i));
        }

        Collections.sort(expected);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        ArrayList<Future<Void>> tasks = new ArrayList<>();

        try {
            for (int i = 0; i < 4; i++) {
                Future<Void> task = executor.submit(() -> {
                    for (int pass = 0; pass < 20; pass++) {
                        list.sortPass(0, 0);
                    }
                    return null;
                });
                tasks.add(task);
            }

            for (Future<Void> task : tasks) {
                task.get(5, TimeUnit.SECONDS);
            }

            assertIterableEquals(expected, list);
            assertEquals(4 * 20 * 19, list.getSteps());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shotsDuringSortingTest() throws Exception {
        MyArrayList list = new MyArrayList();
        ArrayList<String> expected = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            list.addFirst(String.valueOf(i%7));
            expected.add(String.valueOf(i%7));
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        ArrayList<Future<Void>> tasks = new ArrayList<>();

        try {
            for (int i = 0; i < 3; i++) {
                Future<Void> task = executor.submit(() -> {
                    for (int pass = 0; pass < 10; pass++) {
                        list.sortPass(1, 1);
                    }
                    return null;
                });
                tasks.add(task);
            }

            for (int i = 0; i < 20; i++) {
                list.addFirst(String.valueOf(i % 7));
                expected.add(String.valueOf(i % 7));

                ArrayList<String> actual = new ArrayList<>();
                for (String item : list) {
                    actual.add(item);
                }

                Collections.sort(actual);
                Collections.sort(expected);
                assertEquals(expected, actual);
            }

            for (Future<Void> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
