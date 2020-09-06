package var.modified.com.intellij.rt.coverage.data;

import com.google.common.collect.MapMaker;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

public class Redirector {
    public static Map<String, Integer> CLASSES_PATTERNS = new HashMap<>();
    private static final int nrThreads = 1;
    public static Map<Object, Map<String, String[]>> cache;
    public static Map<Object, Map<String, LinkedList<StackFrame>>> varCache;

    public static short[] nrSavesPerClass;
    private static final int maxInstancesPerClass = 1000;
    static ExecutorService executorService = new ThreadPoolExecutor(nrThreads, nrThreads, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    static Map<Object, Map<String, List<MyRunnable>>> runnables;

    static {
        cache = new MapMaker().weakKeys().concurrencyLevel(nrThreads).makeMap();
        varCache = new MapMaker().weakKeys().concurrencyLevel(nrThreads).makeMap();
        runnables = new MapMaker().weakKeys().concurrencyLevel(4).makeMap();
    }

    static class StackFrame {
        public Object value;
        public String[] stackFrames;
    }

    static class MyRunnable implements Runnable {
        private Object value;
        private String fieldName;
        private int index;
        private Throwable throwable;
        private boolean isPrimitive;
        WeakReference<Object> weakReference;

        public MyRunnable(Object obj, Object value, String fieldName, Throwable throwable, boolean isPrimitive, int index) {
            runnables.computeIfAbsent(obj, k -> new LinkedHashMap<>()).computeIfAbsent(fieldName, k -> new ArrayList<>()).add(this);
            weakReference = new WeakReference<>(obj);
            this.value = value;
            this.fieldName = fieldName;
            this.index = index;
            this.throwable = throwable;
            this.isPrimitive = isPrimitive;
        }

        @Override
        public void run() {
            Object object = weakReference.get();
            if (object == null) {
                return;
            }

            nrSavesPerClass[index]++;

            if (isPrimitive) {
                Map<String, LinkedList<StackFrame>> fieldMap = varCache
                        .computeIfAbsent(object, k -> new HashMap<>());

                LinkedList<StackFrame> stackFrames = fieldMap.get(fieldName);
                if (stackFrames == null) {
                    stackFrames = new LinkedList<>();
                }


                StackFrame stackFrame;
                if (stackFrames.size() > 3) {
                    //reuse object
                    stackFrame = stackFrames.removeFirst();
                } else {
                    stackFrame = new StackFrame();
                }

                stackFrame.value = value;
                stackFrame.stackFrames = getStackFrames(throwable);

                stackFrames.addLast(stackFrame);

                fieldMap.put(fieldName, stackFrames);
            } else {
                cache.computeIfAbsent(object, k -> new HashMap<>()).put(fieldName, getStackFrames(throwable));
            }
            throwable = null;
            weakReference = null;
            value = null;
            runnables.get(object).get(fieldName).remove(this);
        }
    }

    private static String[] getStackFrames(Throwable throwable) {
        StackTraceElement[] traces = throwable.getStackTrace();
        List<String> stackFrames = new ArrayList<>();

        for (int i = 1; i < traces.length && i < 5; i++) {
            StackTraceElement trace = traces[i];
            stackFrames.add(trace.getClassName() + "#" + trace.getMethodName() + "#" + trace.getLineNumber());
        }

        return stackFrames.toArray(new String[0]);
    }

    //objects are not saved, else memory consumption would be too large, also can cause issues in the application
    public static void addStackFrameNonBoxed(Object object, Object discardedValue, String fieldName, int index) {
        if (nrSavesPerClass[index] > maxInstancesPerClass) {
            return;
        }

        nrSavesPerClass[index]++;

        executorService.submit(new MyRunnable(object, null, fieldName, new Throwable(), false, index));
        //queue.offer(new MyRunnable(object, null, fieldName, new Throwable(), false, index));
    }

    public static void addStackFrame(Object object, Object value, String fieldName, int index) {
        if (nrSavesPerClass[index] > maxInstancesPerClass) {
            return;
        }

        nrSavesPerClass[index]++;

        executorService.submit(new MyRunnable(object, value, fieldName, new Throwable(), true, index));
        //queue.offer(new MyRunnable(object, value, fieldName, new Throwable(), true, index));
    }


    public static String[] getAccess(Object object, String field) {
        Map<String, String[]> stringThrowableMap = cache.get(object);

        if (stringThrowableMap == null) {
            return getFromQueue(object, field, 0, false);
        }

        return stringThrowableMap.get(field);
    }


    public static String[] getAccessPrimitiveAt(Object object, String field, int index) {
        Map<String, LinkedList<StackFrame>> fieldMap = varCache.get(object);
        if (fieldMap == null) {
            return getFromQueue(object, field, index, false);
        }

        LinkedList<StackFrame> stackFrames = fieldMap.get(field);

        if (stackFrames == null) {
            return getFromQueue(object, field, index, true);
        }

        if (index >= stackFrames.size()) {
            return getFromQueue(object, field, index, true);
        }

        StackFrame stackFrame = stackFrames.get(index);

        List<String> traces = new ArrayList<>(Arrays.asList(stackFrame.stackFrames));
        traces.add(String.valueOf(stackFrame.value));
        return traces.toArray(new String[0]);
    }

    private static String[] getFromQueue(Object object, String field, int index, boolean isPrimitive) {
        MyRunnable finalRunnable = null;

        Map<String, List<MyRunnable>> fieldsMap = runnables.get(object);

        if (fieldsMap == null) {
            return null;
        }

        List<MyRunnable> myRunnables = fieldsMap.get(field);

        if (myRunnables == null) {
            return null;
        }

        for (MyRunnable myRunnable : myRunnables) {
            try {
                Object obj = myRunnable.weakReference.get();
                if (obj != null && field.equals(myRunnable.fieldName)) {
                    finalRunnable = myRunnable;
                    break;
                }
            } catch (Exception e) {
            }
        }

        if (finalRunnable != null) {
            finalRunnable.run();
        }

        if (isPrimitive) {
            return getAccessPrimitiveAt2(object, field, index);
        }
        return getAccess2(object, field, index);
    }

    //prevent stack overflow

    public static String[] getAccess2(Object object, String field, int ind) {
        Map<String, String[]> stringThrowableMap = cache.get(object);

        if (stringThrowableMap == null) {
            return null;
        }

        return stringThrowableMap.get(field);
    }


    public static String[] getAccessPrimitiveAt2(Object object, String field, int index) {
        Map<String, LinkedList<StackFrame>> fieldMap = varCache.get(object);
        if (fieldMap == null) {
            return null;
        }

        LinkedList<StackFrame> stackFrames = fieldMap.get(field);

        if (stackFrames == null) {
            return null;
        }

        if (index >= stackFrames.size()) {
            return null;
        }

        StackFrame stackFrame = stackFrames.get(index);

        List<String> traces = new ArrayList<>(Arrays.asList(stackFrame.stackFrames));
        traces.add(String.valueOf(stackFrame.value));
        return traces.toArray(new String[0]);
    }
}
