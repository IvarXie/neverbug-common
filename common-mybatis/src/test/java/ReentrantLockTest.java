///*
//                            _ooOoo_  
//                           o8888888o  
//                           88" . "88  
//                           (| -_- |)  
//                            O\ = /O  
//                        ____/`---'\____  
//                      .   ' \\| |// `.  
//                       / \\||| : |||// \  
//                     / _||||| -:- |||||- \  
//                       | | \\\ - /// | |  
//                     | \_| ''\---/'' | |  
//                      \ .-\__ `-` ___/-. /  
//                   ___`. .' /--.--\ `. . __  
//                ."" '< `.___\_<|>_/___.' >'"".  
//               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
//                 \ \ `-. \_ __\ /__ _/ .-` / /  
//         ======`-.____`-.___\_____/___.-`____.-'======  
//                            `=---='  
//  
//         .............................................  
//                  佛祖镇楼                  BUG辟易  
//          佛曰:  
//                  写字楼里写字间，写字间里程序员；  
//                  程序人员写程序，又拿程序换酒钱。  
//                  酒醒只在网上坐，酒醉还来网下眠；  
//                  酒醉酒醒日复日，网上网下年复年。  
//                  但愿老死电脑间，不愿鞠躬老板前；  
//                  奔驰宝马贵者趣，公交自行程序员。  
//                  别人笑我忒疯癫，我笑自己命太贱；  
//                  不见满街漂亮妹，哪个归得程序员？
//*/
//
//import org.junit.Test;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * ReentrantLock的测试
// * <p>
// * Created by neverbug
// * Created on 2017/10/11 16:23
// */
//public class ReentrantLockTest {
//
//    private ReentrantLock lock = new ReentrantLock();
//
//    @Test
//    public void reentrantLock()throws Exception{
//        CountDownLatch c = new CountDownLatch(100);
//        for(int i=0;i<2;i++){
//            new Thread(()->{
//                try{
//                    CountDownLatch countDownLatch = new CountDownLatch(1);
//                    System.out.println("lock");
//                    lock.lock();
//                    System.out.println("start");
//                    countDownLatch.await();
//                    lock.unlock();
//                }catch (Exception e){}
//            }).start();
//        }
//        c.await();
//    }
//}
