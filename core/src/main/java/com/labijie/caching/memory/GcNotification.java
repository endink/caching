/**
 * Created with IntelliJ IDEA.
 *
 * @author Anders Xiao
 * @date 2018-08-30
 */
package com.labijie.caching.memory;

import com.sun.management.GarbageCollectionNotificationInfo;
import sun.management.GarbageCollectionNotifInfoCompositeData;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 提供一个注册程序以便在GC收集进行的时候发起回掉。
 * Created by ax03 on 2017/3/6.
 */
public final class GcNotification
{
    private final static String[] OLDGEN_COLLECTOR_NAMES = new String[]{
            // Oracle (Sun) HotSpot
            // -XX:+UseSerialGC
            "MarkSweepCompact",
            // -XX:+UseParallelGC and (-XX:+UseParallelOldGC or -XX:+UseParallelOldGCCompacting)
            "PS MarkSweep",
            // -XX:+UseConcMarkSweepGC
            "ConcurrentMarkSweep",
            // Oracle (BEA) JRockit
            // -XgcPrio:pausetime
            "Garbage collection optimized for short pausetimes Old Collector",
            // -XgcPrio:throughput
            "Garbage collection optimized for throughput Old Collector",
            // -XgcPrio:deterministic
            "Garbage collection optimized for deterministic pausetimes Old Collector"
    };

    private GcNotification()
    {

    }

    public static void Register(Consumer<Object> callback, Object state)
    {
        ManagementFactory.getGarbageCollectorMXBeans().forEach(bean->
        {
            NotificationEmitter notification =  (NotificationEmitter)bean;
            notification.addNotificationListener(new NotificationListener() {
                @Override
                public void handleNotification(Notification notification, Object handback) {
                    String notifType = notification.getType();

                    if (notifType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        CompositeData cd = (CompositeData) notification.getUserData();
                        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);
                        String gcName = info.getGcName();
                        if (Arrays.binarySearch(OLDGEN_COLLECTOR_NAMES, gcName) >= 0) {

                            //System.out.println("FULL gc was notified !");
                            callback.accept(state);
                        }
                    }
                }
            }, null, null);
        });
    }
}
