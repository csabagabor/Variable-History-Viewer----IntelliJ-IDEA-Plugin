package var.modified.com.intellij.rt.coverage.main;

import com.google.common.collect.MapMaker;
import var.modified.com.intellij.rt.coverage.data.Redirector;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CoveragePremain {
    public static void main(String[] args) throws IOException {

//        new Redirector();
//        for (int i = 0; i < 10000000; i++) {
//            long l = System.nanoTime();
//            Redirector.addStackFrame(new CoveragePremain(), 1, "field", 1);
//            long l2 = System.nanoTime();
//            System.out.println(l2-l);
//            System.out.println();
//        }


//        new ClassReader(Redirector.class.getResourceAsStream("Redirector.class"))
//                .accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), 0);
    }

    public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
        premain(argsString, instrumentation, "var.modified.com.intellij.rt.coverage.instrumentation.Instrumentator");
    }

    public static void premain(String argsString, Instrumentation instrumentation, String instrumenterName) throws Exception {
        //System.out.println("loaded history runner agent");
        Class<?> instrumentator = Class.forName(instrumenterName, true, CoveragePremain.class.getClassLoader());
        Method premainMethod = instrumentator.getDeclaredMethod("premain", String.class, Instrumentation.class);
        premainMethod.invoke(null, argsString, instrumentation);
    }
}