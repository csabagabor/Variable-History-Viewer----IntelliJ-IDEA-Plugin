
package var.modified.com.intellij.rt.coverage.instrumentation;

import var.modified.com.intellij.rt.coverage.data.Redirector;
import var.original.com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Instrumentator {

    public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
        try {
            //needed as soon as possible to set breakpoint
            Class.forName("var.modified.com.intellij.rt.coverage.data.Redirector");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        (new Instrumentator()).performPremain(argsString, instrumentation);
    }

    public void performPremain(String patternFile, Instrumentation instrumentation) throws Exception {
        List<Pattern> includePatterns = new ArrayList<>();
        System.out.println("---- Variable History Recorder Agent loaded - version 1.73--- ");

        String[] args;
        try {
            args = this.readArgsFromFile(patternFile);
        } catch (IOException e) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            args = this.readArgsFromFile(patternFile);
        }

        for (int i = 0; i < args.length; ++i) {
            try {
                Redirector.CLASSES_PATTERNS.put(args[i], i);
            } catch (PatternSyntaxException var18) {
                System.err.println("Problem occurred with include pattern " + args[i]);
                System.err.println(var18.getDescription());
                System.err.println("This may cause no tests run and no coverage collected");
                System.exit(1);
            }
        }

        Redirector.nrSavesPerClass = new short[Redirector.CLASSES_PATTERNS.size()];

        List<Pattern> excludePatterns = new ArrayList<>();
        ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
        instrumentation.addTransformer(new CoverageClassfileTransformer(false, excludePatterns, includePatterns, cf),
                true);
    }

    private String[] readArgsFromFile(String arg) throws IOException {
        List<String> result = new ArrayList<>();
        File file = new File(arg);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

        try {
            while (reader.ready()) {
                result.add(reader.readLine());
            }
        } finally {
            reader.close();
        }

        return result.toArray(new String[0]);
    }
}
