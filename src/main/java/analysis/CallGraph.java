package analysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import sootup.core.signatures.MethodSignature;

public class CallGraph {
  @Nonnull public final String algorithm;
  @Nonnull private final Set<MethodSignature> nodes;

  @Nonnull private final Set<Edge<MethodSignature, MethodSignature>> edges;

  public CallGraph(@Nonnull String algorithm) {
    this.algorithm = algorithm;

    nodes = new HashSet<>();
    edges = new HashSet<>();
  }

  public CallGraph(@Nonnull String algorithm, @Nonnull CallGraph cg) {
    this(algorithm);

    nodes.addAll(cg.nodes);
    edges.addAll(cg.edges);
  }

  public void addNode(@Nonnull MethodSignature method) {
    if (nodes.contains(method)) {
      throw new IllegalArgumentException(
          "Call graph already contains method: " + method.toString());
    }
    nodes.add(method);
  }

  public void addEdge(@Nonnull MethodSignature source, @Nonnull MethodSignature target) {
    if (!nodes.contains(source)) {
      throw new IllegalArgumentException(
          "Call graph does not contain source node. Please add source node first. "
              + source.toString());
    }
    if (!nodes.contains(target)) {
      throw new IllegalArgumentException(
          "Call graph does not contain target node. Please add target node first. "
              + target.toString());
    }

    Edge<MethodSignature, MethodSignature> edge = new Edge<>(source, target);
    if (edges.contains(edge)) {
      throw new IllegalArgumentException("Call graph already contains edge: " + edge);
    }

    edges.add(edge);
  }

  public boolean hasNode(@Nonnull MethodSignature m) {
    return nodes.contains(m);
  }

  public boolean hasEdge(@Nonnull MethodSignature source, @Nonnull MethodSignature target) {
    return edges.contains(new Edge<>(source, target));
  }

  @Nonnull
  public Set<MethodSignature> edgesOutOf(@Nonnull MethodSignature origin) {
    if (!nodes.contains(origin)) {
      return Collections.emptySet();
    }

    return edges.stream()
        .filter(edge -> edge.source.equals(origin))
        .map(edge -> edge.target)
        .collect(Collectors.toSet());
  }

  @Nonnull
  public Set<MethodSignature> edgesInto(MethodSignature target) {
    if (!nodes.contains(target)) {
      return Collections.emptySet();
    }

    return edges.stream()
        .filter(edge -> edge.target.equals(target))
        .map(edge -> edge.source)
        .collect(Collectors.toSet());
  }
}
