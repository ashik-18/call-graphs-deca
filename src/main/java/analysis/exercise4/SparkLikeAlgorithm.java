package analysis.exercise4;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;
import analysis.exercise1.CHAAlgorithm;
import javax.annotation.Nonnull;
import sootup.java.core.views.JavaView;

public class SparkLikeAlgorithm extends CallGraphAlgorithm {

  @Override
  @Nonnull
  protected String getAlgorithm() {
    return "SparkLike";
  }

  @Override
  protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {
    CallGraph initialCallGraph = new CHAAlgorithm().constructCallGraph(view);

    // Your implementation goes here, also feel free to add methods as needed
    // To get your entry points we prepared getEntryPoints(view) in the superclass for you

  }
}
