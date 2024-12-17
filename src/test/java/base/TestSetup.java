package base;

import org.junit.Assert;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.signatures.MethodSignature;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.util.Set;

public abstract class TestSetup {
  protected JavaView view;

  public TestSetup() {
    String classPath =
        System.getProperty("user.dir")
            + File.separator
            + "target"
            + File.separator
            + "test-classes/";
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(classPath);
    JavaLanguage language = new JavaLanguage(8);
    view = new JavaView(inputLocation);
    //		view.configBodyInterceptors( aip -> BytecodeClassLoadingOptions.Default );
  }

  protected void assertCallExists(Set<MethodSignature> calledMethods, String methodSignatureStr) {
      final MethodSignature methodSignature =
          view.getIdentifierFactory().parseMethodSignature(methodSignatureStr);
      Assert.assertTrue(calledMethods.contains(methodSignature));
  }

  protected void assertCallMissing(Set<MethodSignature> calledMethods, String methodSignatureStr) {
      final MethodSignature methodSignature =
          view.getIdentifierFactory().parseMethodSignature(methodSignatureStr);
      Assert.assertFalse(calledMethods.contains(methodSignature));
  }
}
