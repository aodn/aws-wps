package au.org.aodn.aws.wps.status;

public class QueuePosition {

    int position;
    int numberInQueue;

    public QueuePosition(int position, int numberInQueue) {
        this.position = position;
        this.numberInQueue = numberInQueue;
    }

    public int getPosition() {
        return position;
    }

    public int getNumberInQueue() {
        return numberInQueue;
    }
}
