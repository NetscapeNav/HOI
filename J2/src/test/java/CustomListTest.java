import CustomList.CustomList;
import Node.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CustomListTest {


    @Test
    public void emptyListTest() throws InterruptedException {
        CustomList list = new CustomList();
        list.sortPass(0, 0);
        assertFalse(list.iterator().hasNext());
        assertEquals(0, list.getSteps());
    }

    @Test
    public void singleElementTest() throws InterruptedException {
        CustomList list = new CustomList();
        Node nodeA = new Node("a", null);
        list.addFirst(nodeA);
        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a"), list);
        assertEquals(0, list.getSteps());
    }

    @Test
    public void sortPassTest() throws InterruptedException {
        CustomList list = new CustomList();
        Node nodeA = new Node("a", null);
        list.addFirst(nodeA);
        Node nodeB = new Node("b", null);
        list.addFirst(nodeB);
        Node nodeC = new Node("c", null);
        list.addFirst(nodeC);
        assertIterableEquals(java.util.List.of("c", "b", "a"), list);

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("b", "a", "c"), list);
        assertEquals(2, list.getSteps());
    }

    @Test
    public void duplicateValuesTest() throws InterruptedException {
        CustomList list = new CustomList();
        Node nodeA = new Node("a", null);
        list.addFirst(nodeA);
        Node nodeB = new Node("b", null);
        list.addFirst(nodeB);
        Node nodeA2 = new Node("a", null);
        list.addFirst(nodeA2);

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a", "a", "b"), list);
        assertEquals(2, list.getSteps());

        list.sortPass(0,0);
        assertIterableEquals(java.util.List.of("a", "a", "b"), list);
        assertEquals(4, list.getSteps());
    }

    @Test
    public void concurrentSortTest() throws Exception {
        CustomList list = new CustomList();
        ArrayList<String> expected = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Node node = new Node(String.valueOf(i), null);
            list.addFirst(node);
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
        CustomList list = new CustomList();
        ArrayList<String> expected = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            Node node = new Node(String.valueOf(i%7), null);
            list.addFirst(node);
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
                list.addFirst(new Node(String.valueOf(i % 7), null));
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
