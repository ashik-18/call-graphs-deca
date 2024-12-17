package exercises;

import analysis.CallGraph;
import analysis.exercise2.RTAAlgorithm;
import base.TestSetup;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import sootup.core.IdentifierFactory;
import sootup.core.signatures.MethodSignature;

public class RTATest extends TestSetup {

  private final CallGraph cg;
  private final MethodSignature interfaceMethod;
  private final MethodSignature superclassMethod;
  private final MethodSignature subclassMethod;
  private final MethodSignature intermediateMethod;
  private final MethodSignature leafMethod;
  private final MethodSignature otherLeafMethod;
  private final MethodSignature specializationMethod;
  private final MethodSignature thirdLeafMethod;
  private final MethodSignature fourthLeafMethod;

  private final MethodSignature mainMethod;

  public RTATest() {
    super();
    final IdentifierFactory identifierFactory = view.getIdentifierFactory();
    interfaceMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.SomeInterface: void doSomething()>");
    superclassMethod =
        identifierFactory.parseMethodSignature("<target.exercise2.Superclass: void doSomething()>");
    subclassMethod =
        identifierFactory.parseMethodSignature("<target.exercise2.Subclass: void doSomething()>");
    intermediateMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.IntermediateClass: void doSomething()>");
    leafMethod =
        identifierFactory.parseMethodSignature("<target.exercise2.LeafClass: void doSomething()>");
    otherLeafMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.OtherLeafClass: void doSomething()>");
    specializationMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.Specialization: void doSomething()>");
    thirdLeafMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.ThirdLeafClass: void doSomething()>");
    fourthLeafMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.FourthLeafClass: void doSomething()>");

    mainMethod =
        identifierFactory.parseMethodSignature(
            "<target.exercise2.Starter: void main(java.lang.String[])>");

    RTAAlgorithm rta = new RTAAlgorithm();
    cg = rta.constructCallGraph(view);
  }

  @Test
  public void genericCall() {
    Set<MethodSignature> callsFromMain = cg.edgesOutOf(mainMethod);

    // things actually instantiated
    Assert.assertTrue(callsFromMain.contains(leafMethod));
    Assert.assertTrue(callsFromMain.contains(otherLeafMethod));
    Assert.assertTrue(callsFromMain.contains(specializationMethod));
    Assert.assertTrue(callsFromMain.contains(subclassMethod));

    // things that aren't instantiated
    Assert.assertFalse(callsFromMain.contains(thirdLeafMethod));
    Assert.assertFalse(callsFromMain.contains(fourthLeafMethod));
  }
}
