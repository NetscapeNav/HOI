package Node;

import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private String value;
    private Node next = null;
    private ReentrantLock lock = new ReentrantLock();

    public Node(String value, Node next) {
        this.value = value;
        this.next = next;
    }

    public String getValue() {
        return value;
    }

    public Node getNext() {
        return next;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void setNext(Node next) {
        this.next = next;
    }
}
