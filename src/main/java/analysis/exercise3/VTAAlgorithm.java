package analysis.exercise3;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.graphstream.algorithm.TarjanStronglyConnectedComponents;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class VTAAlgorithm extends CallGraphAlgorithm {

    private final Logger log = LoggerFactory.getLogger("VTA");

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "VTA";
    }

    @Override
    protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {
        // Get all entry points (methods considered as starting points for the analysis)

    }


    static class Pair<A, B> {
        final A first;
        final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Pair<?, ?> pair = (Pair<?, ?>) o;

            if (!Objects.equals(first, pair.first)) return false;
            return Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (second != null ? second.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ')';
        }
    }

    /**
     * You can use this class to represent your type assignment graph. We do not use this data
     * structure in tests, so you are free to use something else. However, we use this data structure
     * in our solution and it instantly supports collapsing strong-connected components.
     */
    private class TypeAssignmentGraph {
        private final Graph graph;
        private final TarjanStronglyConnectedComponents tscc = new TarjanStronglyConnectedComponents();

        public TypeAssignmentGraph() {
            this.graph = new MultiGraph("tag");
        }

        private boolean containsNode(Value value) {
            return graph.getNode(createId(value)) != null;
        }

        private boolean containsEdge(Value source, Value target) {
            return graph.getEdge(createId(source) + "-" + createId(target)) != null;
        }

        private String createId(Value value) {
            if (value instanceof JFieldRef) return value.toString();
            return Integer.toHexString(System.identityHashCode(value));
        }

        public void addNode(Value value) {
            if (!containsNode(value)) {
                Node node = graph.addNode(createId(value));
                node.setAttribute("value", value);
                node.setAttribute("ui.label", value);
                node.setAttribute("tags", new HashSet<ClassType>());
            }
        }

        public void tagNode(Value value, ClassType classTag) {
            if (containsNode(value)) {
                getNodeTags(value).add(classTag);
            }
        }

        public Set<Pair<Value, Set<ClassType>>> getTaggedNodes() {
            return graph.getNodeSet().stream()
                    .map(
                            n -> new Pair<Value, Set<ClassType>>(n.getAttribute("value"), n.getAttribute("tags")))
                    .filter(p -> p.second.size() > 0)
                    .collect(Collectors.toSet());
        }

        public Set<ClassType> getNodeTags(Value val) {
            return graph.getNode(createId(val)).getAttribute("tags");
        }

        public void addEdge(Value source, Value target) {
            if (!containsEdge(source, target)) {
                Node sourceNode = graph.getNode(createId(source));
                Node targetNode = graph.getNode(createId(target));
                if (sourceNode == null || targetNode == null)
                    log.error(
                            "Could not find one of the nodes. Source: "
                                    + sourceNode
                                    + " - Target: "
                                    + targetNode);
                graph.addEdge(createId(source) + "-" + createId(target), sourceNode, targetNode, true);
            }
        }

        public Set<Value> getTargetsFor(Value initialNode) {
            if (!containsNode(initialNode)) return Collections.emptySet();
            Node source = graph.getNode(createId(initialNode));
            Collection<org.graphstream.graph.Edge> edges = source.getLeavingEdgeSet();
            return edges.stream()
                    .map(e -> (Value) e.getTargetNode().getAttribute("value"))
                    .collect(Collectors.toSet());
        }

        /**
         * Use this method to start the SCC computation.
         */
        public void annotateScc() {
            tscc.init(graph);
            tscc.compute();
        }

        /**
         * Retrieve the index assigned by the SCC algorithm
         *
         * @param value
         * @return
         */
        public Object getSccIndex(Value value) {
            if (!containsNode(value)) return null;
            return graph.getNode(createId(value)).getAttribute(tscc.getSCCIndexAttribute());
        }

        /**
         * Use this method to inspect your type assignment graph
         */
        public void draw() {
            graph.display();
        }
    }
}
