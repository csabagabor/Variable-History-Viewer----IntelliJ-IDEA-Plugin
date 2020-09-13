package var.modified.com.intellij.rt.coverage.data;

import com.google.common.collect.MapMaker;

import java.util.*;

public class Redirector {
    public static Map<String, Integer> CLASSES_PATTERNS = new HashMap<>();
    private static final int nrThreads = 4;
    public static Map<Object, Map<String, Throwable>> cache;
    public static Map<Object, Map<String, LinkedList<StackFrame>>> varCache;

    public static short[] nrSavesPerClass;
    private static final int maxInstancesPerClass = 1000;

    static {
        cache = new MapMaker().weakKeys().concurrencyLevel(nrThreads).makeMap();
        varCache = new MapMaker().weakKeys().concurrencyLevel(nrThreads).makeMap();
    }

    static class StackFrame {
        public Object value;
        public Throwable throwable;
    }


    //objects are not saved, else memory consumption would be too large, also can cause issues in the application
    public static void addStackFrameNonBoxed(Object object, Object discardedValue, String fieldName, int index) {
        try {
            if (nrSavesPerClass[index] > maxInstancesPerClass) {
                return;
            }

            nrSavesPerClass[index]++;
            synchronized (object) {
                cache.computeIfAbsent(object, k -> new HashMap<>()).put(fieldName, new Throwable());
            }
        } catch (Exception e) {
        }
    }

    public static void addStackFrame(Object object, Object value, String fieldName, int index) {
        try {
            if (nrSavesPerClass[index] > maxInstancesPerClass) {
                return;
            }

            nrSavesPerClass[index]++;
            synchronized (object) {

                Map<String, LinkedList<StackFrame>> fieldMap = varCache
                        .computeIfAbsent(object, k -> new HashMap<>());

                LinkedList<StackFrame> stackFrames = fieldMap.get(fieldName);
                if (stackFrames == null) {
                    stackFrames = new LinkedList<>();
                }


                StackFrame stackFrame;
                if (stackFrames.size() > 2) {
                    //reuse object
                    stackFrame = stackFrames.removeFirst();
                } else {
                    stackFrame = new StackFrame();
                }

                stackFrame.value = value;
                stackFrame.throwable = new Throwable();

                stackFrames.addLast(stackFrame);

                fieldMap.put(fieldName, stackFrames);
            }
        } catch (Exception e) {
        }
    }


    public static String[] getAccess(Object object, String field) {
        Map<String, Throwable> stringThrowableMap = cache.get(object);

        if (stringThrowableMap == null) {
            return null;
        }

        return getStackFrames(stringThrowableMap.get(field));
    }


    public static String[] getAccessPrimitiveAt(Object object, String field, int index) {
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

        List<String> traces = new ArrayList<>(Arrays.asList(getStackFrames(stackFrame.throwable)));
        traces.add(String.valueOf(stackFrame.value));
        return traces.toArray(new String[0]);
    }

    private static String[] getStackFrames(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StackTraceElement[] traces = throwable.getStackTrace();
        List<String> stackFrames = new ArrayList<>();

        for (int i = 1; i < traces.length; i++) {
            StackTraceElement trace = traces[i];
            stackFrames.add(trace.getClassName() + "#" + trace.getMethodName() + "#" + trace.getLineNumber());
        }

        return stackFrames.toArray(new String[0]);
    }
}
