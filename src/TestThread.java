public class TestThread extends Thread{
    int count;
    TestThread(int count) {
        this.count = count;
    }

    @Override
    public void run() {
        for(int i = 0; i < 100; i++) {
            count++;
            System.out.println(count);
        }
    }

    public static void main(String[] args) {
        TestThread t1 = new TestThread(0);
        TestThread t2 = new TestThread(1000);
        t1.start();
        t2.start();
    }
}


