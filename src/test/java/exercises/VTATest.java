package exercises;

import analysis.CallGraph;
import analysis.exercise3.VTAAlgorithm;
import base.TestSetup;
import java.util.Set;
import org.junit.Test;
import sootup.core.signatures.MethodSignature;

public class VTATest extends TestSetup {
  private final CallGraph cg;
  private final MethodSignature scenarioMain;

  public VTATest() {
    super();
    scenarioMain =
        view.getIdentifierFactory()
            .parseMethodSignature(
                "<target.exercise3.SimpleScenario: void main(java.lang.String[])>");

    VTAAlgorithm vta = new VTAAlgorithm();
    cg = vta.constructCallGraph(view);
  }

  @Test
  public void testScenario() {
    Set<MethodSignature> callsFromMain = cg.edgesOutOf(scenarioMain);
    assertCallExists(callsFromMain, "<target.exercise2.LeafClass: void doSomething()>");
    assertCallExists(callsFromMain, "<target.exercise2.FifthLeafClass: void doSomething()>");
    assertCallExists(callsFromMain, "<target.exercise2.SixthLeafClass: void doSomething()>");

    assertCallMissing(callsFromMain, "<target.exercise2.SomeInterface: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.FourthLeafClass: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.IntermediateClass: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.OtherLeafClass: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.Specialization: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.Subclass: void doSomething()>");
    assertCallMissing(callsFromMain, "<target.exercise2.Superclass: void doSomething()>");
  }
}
