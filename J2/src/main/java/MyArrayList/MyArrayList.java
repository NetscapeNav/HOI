package MyArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MyArrayList implements Iterable<String> {
    private List<String> list = Collections.synchronizedList(new ArrayList<>());
    private AtomicLong steps = new AtomicLong();

    public void addFirst(String value) {
        list.add(0, value);
    }

    public void sortPass(long insideDelay, long betweenDelay) throws InterruptedException {
        int i = 0;
        while (true) {
            synchronized (list) {
                if (i+1 >= list.size()) {
                    break;
                }
                Thread.sleep(insideDelay);
                steps.incrementAndGet();
                String first = list.get(i);
                String second = list.get(i+1);
                if (first.compareTo(second) > 0) {
                    list.set(i, second);
                    list.set(i+1, first);
                }
            }
            Thread.sleep(betweenDelay);
            i++;
        }
    }

    public long getSteps() {
        return steps.get();
    }

    @Override
    public Iterator<String> iterator() {
        synchronized (list) {
            ArrayList<String> copy = new ArrayList<>(list);
            return copy.iterator();
        }
    }
}
