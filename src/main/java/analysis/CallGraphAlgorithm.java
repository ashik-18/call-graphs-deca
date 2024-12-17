package analysis;

import java.util.stream.Stream;
import javax.annotation.Nonnull;
import sootup.core.model.SootClassMember;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.views.JavaView;

public abstract class CallGraphAlgorithm {
  protected JavaView view;

  @Nonnull
  public CallGraph constructCallGraph(@Nonnull JavaView view) {
    this.view = view;
    CallGraph cg = new CallGraph(getAlgorithm());
    populateCallGraph(view, cg);
    return cg;
  }

  @Nonnull
  protected Stream<MethodSignature> getEntryPoints(@Nonnull JavaView view) {
    return view.getClasses().stream()
        .flatMap(c -> c.getMethods().stream())
        .filter(m -> m.getName().contains("main") && m.hasBody())
        .map(SootClassMember::getSignature);
  }

  protected abstract void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg);

  @Nonnull
  protected abstract String getAlgorithm();
}
