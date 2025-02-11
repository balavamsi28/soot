package soot.util.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;

import soot.*;
import soot.baf.DoubleWordType;
import soot.options.Options;
import soot.tagkit.*;

/**
 * Utility class for ASM-based back-ends.
 */
public class ASMBackendUtils {

    private static final Map<Class<? extends Type>, Character> TYPE_DESC_MAP = new HashMap<>();

    static {
        TYPE_DESC_MAP.put(DoubleType.class, 'D');
        TYPE_DESC_MAP.put(FloatType.class, 'F');
        TYPE_DESC_MAP.put(IntType.class, 'I');
        TYPE_DESC_MAP.put(ByteType.class, 'B');
        TYPE_DESC_MAP.put(ShortType.class, 'S');
        TYPE_DESC_MAP.put(CharType.class, 'C');
        TYPE_DESC_MAP.put(BooleanType.class, 'Z');
        TYPE_DESC_MAP.put(LongType.class, 'J');
        TYPE_DESC_MAP.put(VoidType.class, 'V');
    }

    /**
     * Convert class identifiers and signatures by replacing dots with slashes.
     */
    public static String slashify(String s) {
        return (s != null) ? s.replace('.', '/') : null;
    }

    /**
     * Compute type description for methods.
     */
    public static String toTypeDesc(SootMethodRef m) {
        return toTypeDesc(m.parameterTypes(), m.returnType());
    }

    /**
     * Compute type description for methods.
     */
    public static String toTypeDesc(List<Type> parameterTypes, Type returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        parameterTypes.forEach(t -> sb.append(toTypeDesc(t)));
        sb.append(')').append(toTypeDesc(returnType));
        return sb.toString();
    }

    /**
     * Convert type to JVM-style type description.
     */
  

    public static String toTypeDesc(Type type){
      if(type instanceof ArrayType){
        return "[" + toTypeDesc(((ArrayType) type).getElementType());
    } else if (type instanceof RefType) {
        return "L" + slashify(((RefType) type).getClassName()) + ";";
    } else {
        return String.valueOf(TYPE_DESC_MAP.getOrDefault(type.getClass(), ' '));
      }
    }

    /**
     * Get default value of a field for constant pool.
     */
    public static Object getDefaultValue(SootField field) {
        for (Tag t : field.getTags()) {
            if (t instanceof IntegerConstantValueTag) {
                return ((IntegerConstantValueTag) t).getIntValue();
            } else if (t instanceof LongConstantValueTag) {
                return ((LongConstantValueTag) t).getLongValue();
            } else if (t instanceof FloatConstantValueTag) {
                return ((FloatConstantValueTag) t).getFloatValue();
            } else if (t instanceof DoubleConstantValueTag) {
                return ((DoubleConstantValueTag) t).getDoubleValue();
            } else if (t instanceof StringConstantValueTag && acceptsStringInitialValue(field)) {
                return ((StringConstantValueTag) t).getStringValue();
            }
        }
        return null;
    }

    /**
     * Check if the field accepts a string default value.
     */
    public static boolean acceptsStringInitialValue(SootField field) {
        return (field.getType() instanceof RefType) &&
                "java.lang.String".equals(((RefType) field.getType()).getClassName());
    }

    /**
     * Get the size in words for a type.
     */
    public static int sizeOfType(Type t) {
        return (t instanceof DoubleWordType || t instanceof LongType || t instanceof DoubleType) ? 2
                : (t instanceof VoidType) ? 0 : 1;
    }

    /**
     * Create an ASM attribute from a Soot attribute.
     */
    public static org.objectweb.asm.Attribute createASMAttribute(Attribute attr) {
        return new org.objectweb.asm.Attribute(attr.getName()) {
            @Override
            protected ByteVector write(final ClassWriter cw, final byte[] code, final int len, final int maxStack,
                                       final int maxLocals) {
                return new ByteVector().putByteArray(attr.getValue(), 0, attr.getValue().length);
            }
        };
    }

    /**
     * Translate internal numbering of Java versions to real versions for debug messages.
     */
    public static String translateJavaVersion(int javaVersion) {
        return "1." + (javaVersion - 1);
    }
}