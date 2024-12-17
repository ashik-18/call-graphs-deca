package exercises;

import analysis.CallGraph;
import analysis.exercise1.CHAAlgorithm;
import base.TestSetup;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import sootup.core.IdentifierFactory;
import sootup.core.signatures.MethodSignature;

public class CHATest extends TestSetup {

  private final CallGraph cg;
  private final MethodSignature exampleMain;
  private final MethodSignature exampleConstructor;
  private final MethodSignature subjectConstructor;
  private final MethodSignature exampleSubjectModify;
  private final MethodSignature observableNotifyObservers;
  private final MethodSignature observableNotifyObserversSpecific;
  private final MethodSignature observerUpdate;
  private final MethodSignature exampleUpdate;

  public CHATest() {

    final IdentifierFactory identifierFactory = view.getIdentifierFactory();
    exampleMain =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.SimpleExample: void main(java.lang.String[])>");
    exampleConstructor =
        identifierFactory.parseMethodSignature("<target.exercise1.SimpleExample: void <init>()>");
    subjectConstructor =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.SimpleExample$Subject: void <init>()>");
    exampleSubjectModify =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.SimpleExample$Subject: void modify()>");
    observableNotifyObservers =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.Observable: void notifyObservers()>");
    observableNotifyObserversSpecific =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.Observable: void notifyObservers(java.lang.Object)>");
    observerUpdate =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.Observer: void update(target.exercise1.Observable,java.lang.Object)>");
    exampleUpdate =
        identifierFactory.parseMethodSignature(
            "<target.exercise1.SimpleExample: void update(target.exercise1.Observable,java.lang.Object)>");

    CHAAlgorithm cha = new CHAAlgorithm();
    cg = cha.constructCallGraph(view);
  }

  @Test
  public void constructorCalls() {
    // static to constructor call
    Set<MethodSignature> calledFromMain = cg.edgesOutOf(exampleMain);
    Assert.assertTrue(calledFromMain.contains(exampleConstructor));
    Assert.assertTrue(calledFromMain.contains(subjectConstructor));
  }

  @Test
  public void staticToInstanceCall() {
    Assert.assertTrue(view.getClass(exampleSubjectModify.getDeclClassType()).isPresent());
    Assert.assertTrue(view.getMethod(exampleSubjectModify).isPresent());

    // static to instance call
    Set<MethodSignature> calledFromMain = cg.edgesOutOf(exampleMain);
    Assert.assertTrue(calledFromMain.contains(exampleSubjectModify));
  }

  @Test
  public void instanceToInterfaceMethod() {
    // instance to interface method
    Set<MethodSignature> calledFromModify = cg.edgesOutOf(exampleSubjectModify);
    Assert.assertTrue(calledFromModify.contains(observableNotifyObservers));

    // more specific
    Set<MethodSignature> calledFromNotify = cg.edgesOutOf(observableNotifyObservers);
    Assert.assertTrue(calledFromNotify.contains(observableNotifyObserversSpecific));
  }

  @Test
  public void polymorphicCallSite() {

    //		System.out.println(view.getMethod( observableNotifyObserversSpecific
    // ).get().getBody().getStmtGraph());

    // polymorphic call site (interface)
    Set<MethodSignature> calledMethods = cg.edgesOutOf(observableNotifyObserversSpecific);
    Assert.assertTrue(calledMethods.contains(exampleUpdate));
    Assert.assertTrue(calledMethods.contains(observerUpdate));
  }
}
