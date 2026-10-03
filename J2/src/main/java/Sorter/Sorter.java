package Sorter;

import CustomList.CustomList;

public class Sorter implements Runnable {
    private CustomList list;
    private long insideDelay;
    private long betweenDelay;

    public Sorter(CustomList list) {
        this.list = list;
        insideDelay = 1000L;
        betweenDelay = 1000L;
    }

    public Sorter(CustomList list, long insideDelay, long betweenDelay) {
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
