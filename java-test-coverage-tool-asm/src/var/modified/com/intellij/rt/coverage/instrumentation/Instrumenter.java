package var.modified.com.intellij.rt.coverage.instrumentation;

import org.jetbrains.coverage.org.objectweb.asm.FieldVisitor;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Instrumenter extends ClassVisitor {
    protected final ClassVisitor myClassVisitor;
    private final String myClassName;
    private final boolean myShouldCalculateSource;
    protected boolean myProcess;
    private boolean myEnum;

    public Instrumenter(ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
        super(458752, classVisitor);
        this.myClassVisitor = classVisitor;
        this.myClassName = className;
        this.myShouldCalculateSource = shouldCalculateSource;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.myEnum = (access & 16384) != 0;
        this.myProcess = (access & 512) == 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv == null) {
            return null;
        } else if ((access & 64) != 0) {
            return mv;
        } else if ((access & 1024) != 0) {
            return mv;
        } else if (this.myEnum && isDefaultEnumMethod(name, desc, signature, this.myClassName)) {
            return mv;
        } else {
            this.myProcess = true;
            try {
                return this.createMethodLineEnumerator(mv, name, desc, access, signature, exceptions);
            } catch (Exception e) {
                return mv;
            }
        }
    }

    private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
        return name.equals("values") && desc.equals("()[L" + className + ";") || name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") || name.equals("<init>") && signature != null && signature.equals("()V");
    }

    protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor var1, String var2, String var3, int var4, String var5, String[] var6);

}
