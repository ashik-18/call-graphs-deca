package analysis.exercise3;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import analysis.exercise1.CHAAlgorithm;
import org.graphstream.algorithm.TarjanStronglyConnectedComponents;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.jimple.basic.JavaLocal;
import sootup.java.core.views.JavaView;

public class VTAAlgorithm extends CallGraphAlgorithm {

    private final Logger log = LoggerFactory.getLogger("VTA");

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "VTA";
    }

    @Override
    public void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {

        final CHAAlgorithm chaCg  = new CHAAlgorithm();
        chaCg.populateCallGraph(view, cg);

        final Collection<MethodSignature> entryPoints = getEntryPoints(view).collect(Collectors.toList());
        cg.getNodes().clear();
        cg.getEdges().clear();

        for (final MethodSignature entryPoint : entryPoints) {
            processMethod(entryPoint, view, cg);
        }
    }

    private void processMethod(final @Nonnull MethodSignature method, final @Nonnull JavaView view, final @Nonnull CallGraph cg) {

        //create TAG for every var and then, iterate again to process the invokes

        final Optional<JavaSootMethod> javaMethodOpt = view.getMethod(method);
        if (!javaMethodOpt.isPresent()) {
            return;
        }

        final JavaSootMethod javaMethod = javaMethodOpt.get();
        if (!javaMethod.hasBody()) {
            return;
        }

        final TypeAssignmentGraph tag = new TypeAssignmentGraph();

        for (final Stmt instruction : javaMethod.getBody().getStmts()) {

            MethodSignature invocation = null;
            if (instruction instanceof JAssignStmt) {
                final JAssignStmt assignStmt = (JAssignStmt) instruction;
                Value leftOp = assignStmt.getLeftOp();
                tag.addNode(leftOp);
                if (assignStmt.getRightOp() instanceof JNewExpr) {
                    tag.tagNode(leftOp, ((JNewExpr) assignStmt.getRightOp()).getType());
                }
                if (assignStmt.getRightOp() instanceof JavaLocal) {
                    tag.addNode(assignStmt.getRightOp());
                    if (!tag.containsEdge(assignStmt.getRightOp(), leftOp)) {
                        tag.addEdge(assignStmt.getRightOp(), leftOp);
                    }
                }
                if (assignStmt.getRightOp() instanceof JStaticInvokeExpr) {
                    invocation = ((JStaticInvokeExpr) assignStmt.getRightOp()).getMethodSignature();
                    if (view.getMethod(invocation).isPresent()) {
                        for (final Stmt staticInstruction : view.getMethod(invocation).get().getBody().getStmts()) {
                            if (staticInstruction instanceof JInvokeStmt) {
                                if (staticInstruction.getInvokeExpr().getMethodSignature().getName().equals("<init>")) {
                                    tag.tagNode(leftOp, staticInstruction.getInvokeExpr().getMethodSignature().getDeclClassType());
                                }
                            }

                        }
                    }
                }
                if (assignStmt.getRightOp() instanceof JCastExpr) {
                    tag.tagNode(leftOp, (ClassType) assignStmt.getRightOp().getType());
                    if (!tag.containsEdge(((JCastExpr) assignStmt.getRightOp()).getOp(), leftOp)) {
                        tag.addEdge(((JCastExpr) assignStmt.getRightOp()).getOp(), leftOp);
                    }
                }
                if (assignStmt.getRightOp() instanceof JStaticFieldRef) {
                    if (!tag.containsEdge(assignStmt.getRightOp(), leftOp)) {
                        tag.addEdge(assignStmt.getRightOp(), leftOp);
                    }
                }
            }
        }

        tag.annotateScc();

        //propagating the types of the nodes to the targets
        tag.graph.getNodeSet().stream().collect(Collectors.toList()).forEach(n -> {
            Set<Value> tars = tag.getTargetsFor(n.getAttribute("value"));
            tars.stream().collect(Collectors.toList()).forEach(t -> {
                tag.tagNode(t, tag.getNodeTags(n.getAttribute("value")).stream().collect(Collectors.toList()).get(0));
            });
        });

//        final List<String> temp = new ArrayList<>();
//        tag.graph.getNodeSet().stream().collect(Collectors.toList()).forEach(n -> {
//            temp.add(n.getAttribute("value").toString());
//        });
//        tag.graph.getEdgeSet().stream().collect(Collectors.toList()).forEach(n -> {
//            temp.add(n.getSourceNode().getAttribute("value").toString()+"--" +n.getTargetNode().getAttribute("value").toString());
//        });

        tag.getTaggedNodes();
//        cg.getNodes().clear();
//        cg.getEdges().clear();

        processInvokes(view, method, cg, tag);

    }

    private void processInvokes(final @Nonnull JavaView view, final @Nonnull MethodSignature method, final @Nonnull CallGraph cg, final TypeAssignmentGraph tag) {

        final Optional<JavaSootMethod> javaMethodOpt = view.getMethod(method);
        if (!javaMethodOpt.isPresent()) {
            return;
        }

        final JavaSootMethod javaMethod = javaMethodOpt.get();
        if (!javaMethod.hasBody()) {
            return;
        }

        for (final Stmt instruction : javaMethod.getBody().getStmts()) {

            if (instruction instanceof JInvokeStmt) {
                if (instruction.getInvokeExpr().getMethodSignature().getName().equals("<init>")) {
                    continue;
                }
                if (!cg.hasNode(method)) {
                    cg.addNode(method);
                }
                final JInvokeStmt invokeStmt = (JInvokeStmt) instruction;
                final Value base = invokeStmt.getInvokeExpr().getUses().collect(Collectors.toList()).get(0);

                //creating cg edge for the base
                final Set<ClassType> baseClassTypes = tag.getNodeTags(base);
                addCgEdge(cg, baseClassTypes, invokeStmt, method);

                //creating cg edge for all the targets of the base (for alias)
                final Set<Value> targets = tag.getTargetsFor(base);
                for (final Value target : targets) {
                    final Set<ClassType> classTypes = tag.getNodeTags(target);
                    addCgEdge(cg, classTypes, invokeStmt, method);
                }
            }
        }
    }

    private void addCgEdge(final CallGraph cg, final Set<ClassType> classTypes, final JInvokeStmt invokeStmt, final MethodSignature method) {
        for (final ClassType classType : classTypes) {
            final MethodSignature newMethodSignature = formNewMethodSignature(invokeStmt.getInvokeExpr().getMethodSignature(), classType);
            if (!cg.hasNode(newMethodSignature)) {
                cg.addNode(newMethodSignature);
            }
            if (!cg.hasEdge(method, newMethodSignature)) {
                cg.addEdge(method, newMethodSignature);
            }
        }
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

    private MethodSignature formNewMethodSignature(final MethodSignature original, final ClassType newDeclClassType) {
        return new MethodSignature(
                newDeclClassType,
                original.getName(),
                original.getParameterTypes(),
                original.getType()
        );
    }

}