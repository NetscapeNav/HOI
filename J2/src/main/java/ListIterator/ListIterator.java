package ListIterator;

import Node.Node;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListIterator implements Iterator<String> {
    private Node current;

    public ListIterator(Node current) {
        this.current = current;
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    @Override
    public String next() {
        if (current == null) {
            throw new NoSuchElementException();
        } else {
            String string = current.getValue();
            current = current.getNext();
            return string;
        }
    }
}
