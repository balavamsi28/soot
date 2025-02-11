package soot.util.backend;

import static soot.util.backend.ASMBackendUtils.slashify;

import org.objectweb.asm.ClassWriter;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.Type;

/**
 * @author Tobias Hamann, Florian Kuebler, Dominik Helm, Lukas Sommer
 * ASM class writer with soot-specific resolution of common superclasses.
 */
public class SootASMClassWriter extends ClassWriter {

  /**
   * Constructs a new {@link ClassWriter} object.
   *
   * @param flags option flags to modify default behavior. See {@link #COMPUTE_MAXS}, {@link #COMPUTE_FRAMES}.
   */
  public SootASMClassWriter(int flags) {
    super(flags);
  }

  /**
   * Gets the common superclass for two types.
   */
  @Override
  protected String getCommonSuperClass(String type1, String type2) {
    String typeName1 = type1.replace('/', '.');
    String typeName2 = type2.replace('/', '.');

    Scene scene = Scene.v(); // Store reference to Scene singleton
    SootClass s1, s2;

    try {
      s1 = scene.getSootClass(typeName1);
      s2 = scene.getSootClass(typeName2);
    } catch (RuntimeException e) {
      // If class resolution fails, fall back to java.lang.Object
      return slashify(scene.getObjectType().toString());
    }

    // Short-circuit check for phantom or dangling classes
    if (s1.isPhantom() || s2.isPhantom() || s1.resolvingLevel() == SootClass.DANGLING || s2.resolvingLevel() == SootClass.DANGLING) {
      return slashify(scene.getObjectType().toString());
    }

    // Merge types
    Type mergedType = s1.getType().merge(s2.getType(), scene);

    if (mergedType instanceof RefType) {
      return slashify(((RefType) mergedType).getClassName());
    }

    throw new RuntimeException("Could not find common superclass for " + typeName1 + " and " + typeName2);
  }
}
