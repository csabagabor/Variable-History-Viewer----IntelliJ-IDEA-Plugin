

package var.modified.com.intellij.rt.coverage.instrumentation;

import var.modified.com.intellij.rt.coverage.data.Redirector;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.Set;

public class SamplingInstrumenter extends Instrumenter {
    private static Set<String> BOXED_TYPES = new HashSet<>();

    static {
        BOXED_TYPES.add("Ljava/lang/Boolean;");
        BOXED_TYPES.add("Ljava/lang/Integer;");
        BOXED_TYPES.add("Ljava/lang/Long;");
        BOXED_TYPES.add("Ljava/lang/Float;");
        BOXED_TYPES.add("Ljava/lang/Double;");
        BOXED_TYPES.add("Ljava/lang/Short;");
        BOXED_TYPES.add("Ljava/lang/Character;");
        BOXED_TYPES.add("Ljava/lang/Byte;");
        BOXED_TYPES.add("Ljava/lang/String;");
    }

    public SamplingInstrumenter(ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
        super(classVisitor, className, shouldCalculateSource);
    }

    protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, final String fieldName, final String desc, int access, String signature, String[] exceptions) {
        return new MethodVisitor(458752, mv) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                try {
                    if (opcode == Opcodes.PUTFIELD) {
                        Integer index = Redirector.CLASSES_PATTERNS.get(owner.replace("/", "."));
                        if (index != null && name != null && descriptor != null) {
                            saveToCache(name, descriptor, this.mv, index);
                        }
                    }
                } catch (Exception e) {
                }
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
        };
    }

    private void saveToCache(String fieldName, String signature, MethodVisitor mv, Integer index) {
        Type tp = Type.getType(signature);


        boolean primitive = isPrimitive(tp);
        if (primitive) {
            if (tp.equals(Type.BOOLEAN_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (tp.equals(Type.BYTE_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (tp.equals(Type.CHAR_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (tp.equals(Type.SHORT_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (tp.equals(Type.INT_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (tp.equals(Type.LONG_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (tp.equals(Type.FLOAT_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (tp.equals(Type.DOUBLE_TYPE)) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            }
        }


        mv.visitInsn(Opcodes.DUP2);
        mv.visitLdcInsn(fieldName);
        if (index <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, index);
        } else {
            mv.visitLdcInsn(index);
        }

        if (primitive || BOXED_TYPES.contains(signature)) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "var/modified/com/intellij/rt/coverage/data/Redirector", "addStackFrame", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V", false);
        } else {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "var/modified/com/intellij/rt/coverage/data/Redirector", "addStackFrameNonBoxed", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V", false);
        }

        if (primitive) {
            switch (tp.getSort()) {
                case Type.BOOLEAN:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                    break;
                case Type.BYTE:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                    break;
                case Type.CHAR:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                    break;
                case Type.SHORT:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                    break;
                case Type.INT:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                    break;
                case Type.FLOAT:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                    break;
                case Type.LONG:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                    break;
                case Type.DOUBLE:
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                    break;
            }
        }

    }

    private boolean isPrimitive(Type tp) {
        return Type.BOOLEAN_TYPE.equals(tp) || Type.BYTE_TYPE.equals(tp) || Type.CHAR_TYPE.equals(tp) || Type.SHORT_TYPE.equals(tp) || Type.INT_TYPE.equals(tp)
                || Type.LONG_TYPE.equals(tp) || Type.FLOAT_TYPE.equals(tp) || Type.DOUBLE_TYPE.equals(tp);
    }
}
