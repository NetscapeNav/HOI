package CustomList;

import Node.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class CustomList implements Iterable<String> {
    private Node head = new Node(null, null);
    private AtomicLong steps = new AtomicLong();

    public void addFirst(String value) {
        ReentrantLock lock = head.getLock();
        lock.lock();
        Node node = new Node(value, null);
        try {
            node.setNext(head.getNext());
            head.setNext(node);
        } finally {
            lock.unlock();
        }
    }

    public void addFirst(Node node) {
        ReentrantLock lock = head.getLock();
        lock.lock();
        try {
            node.setNext(head.getNext());
            head.setNext(node);
        } finally {
            lock.unlock();
        }
    }

    private void swapNodes(Node previous, Node first, Node second) {
        first.setNext(second.getNext());
        second.setNext(first);
        previous.setNext(second);
    }

    public void sortPass(long insideDelay, long betweenDelay) throws InterruptedException {
        int index = 0;

        while (true) {
            Node previous = head;
            previous.getLock().lockInterruptibly();

            try {
                for (int i = 0; i < index; i++) {
                    Node next = previous.getNext();
                    if (next == null) {
                        return;
                    }
                    next.getLock().lockInterruptibly();

                    previous.getLock().unlock();
                    previous = next;
                }

                Node first = previous.getNext();
                if (first == null) {
                    return;
                }
                first.getLock().lockInterruptibly();
                try {
                    Node second = first.getNext();
                    if (second == null) {
                        return;
                    }
                    second.getLock().lockInterruptibly();
                    try {
                        Thread.sleep(insideDelay);
                        steps.incrementAndGet();
                        if (first.getValue().compareTo(second.getValue()) > 0) {
                            swapNodes(previous, first, second);
                        }
                    } finally {
                        second.getLock().unlock();
                    }
                } finally {
                    first.getLock().unlock();
                }
            } finally {
                previous.getLock().unlock();
            }

            Thread.sleep(betweenDelay);
            index++;
        }
    }

    public StringBuilder print() {
        StringBuilder result = new StringBuilder();
        for (String value : this) {
            result.append(value).append('\n');
        }
        return result;
    }

    public ArrayList<String> shot() {
        ArrayList<String> result = new ArrayList<>();
        Node current = head;
        current.getLock().lock();

        try {
            while (true) {
                Node next = current.getNext();
                if (next == null) {
                    return result;
                }

                next.getLock().lock();

                Node oldCurrent = current;
                current = next;
                oldCurrent.getLock().unlock();

                result.add(current.getValue());
            }
        } finally {
            current.getLock().unlock();
        }
    }

    public Node getHead() {
        return head;
    }

    public long getSteps() {
        return steps.get();
    }

    @Override
    public Iterator<String> iterator() {
        ArrayList<String> copy = shot();
        return copy.iterator();
    }
}
