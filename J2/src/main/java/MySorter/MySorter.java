package MySorter;

import MyArrayList.MyArrayList;

public class MySorter implements Runnable {
    private MyArrayList list;
    private long insideDelay;
    private long betweenDelay;

    public MySorter(MyArrayList list) {
        this.list = list;
        this.insideDelay = 1000L;
        this.betweenDelay = 1000L;
    }

    public MySorter(MyArrayList list, long insideDelay, long betweenDelay) {
        this.list = list;
        this.insideDelay = insideDelay;
        this.betweenDelay = betweenDelay;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long stepsBefore = list.getSteps();
                list.sortPass(insideDelay, betweenDelay);
                if (list.getSteps() == stepsBefore) {
                    Thread.sleep(Math.max(betweenDelay, 1));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
