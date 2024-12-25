package analysis.exercise2;

import analysis.CallGraph;
import analysis.exercise1.CHAAlgorithm;

import javax.annotation.Nonnull;

import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.jimple.basic.JavaLocal;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.*;
import java.util.stream.Collectors;

public class RTAAlgorithm extends CHAAlgorithm {

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "RTA";
    }

    @Override
    public void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {

        final Collection<MethodSignature> entryPoints = getEntryPoints(view).filter(e -> e.toString().contains("exercise2")).collect(Collectors.toList());

        for (final MethodSignature entryPoint : entryPoints) {
            processMethod(entryPoint, view, cg);
        }
    }

    private void processMethod(final @Nonnull MethodSignature method, final @Nonnull JavaView view, final @Nonnull CallGraph cg) {

        final Optional<JavaSootMethod> javaMethodOpt = view.getMethod(method);
        if (!javaMethodOpt.isPresent()) {
            return;
        }

        final JavaSootMethod javaMethod = javaMethodOpt.get();
        if (!javaMethod.hasBody()) {
            return;
        }
        final Map<JavaLocal, List<JavaClassType>> declaringClassesMap = new HashMap<>();

        for (final Stmt instruction : javaMethod.getBody().getStmts()) {

            MethodSignature invocation = null;
            if (instruction instanceof JAssignStmt) {
                final JAssignStmt assignStmt = (JAssignStmt) instruction;
                if (assignStmt.getRightOp() instanceof JStaticInvokeExpr) {
                    invocation = ((JStaticInvokeExpr) assignStmt.getRightOp()).getMethodSignature();

                    declaringClassesMap.put((JavaLocal) assignStmt.getLeftOp(), new ArrayList<>());
                    if (view.getMethod(invocation).isPresent()) {
                        for (final Stmt staticInstruction : view.getMethod(invocation).get().getBody().getStmts()) {

                            if (staticInstruction instanceof JInvokeStmt) {
                                if (staticInstruction.getInvokeExpr().getMethodSignature().getName().equals("<init>")) {
                                    declaringClassesMap.get(assignStmt.getLeftOp()).add((JavaClassType) staticInstruction.getInvokeExpr().getMethodSignature().getDeclClassType());
                                }
                            }

                        }
                    }
                }
            }
            if (instruction instanceof JInvokeStmt) {

                if (instruction.getInvokeExpr().getMethodSignature().getName().equals("<init>")) {
                    final List<JavaClassType> declaringClassList = new ArrayList<>();
                    declaringClassList.add((JavaClassType) instruction.getInvokeExpr().getMethodSignature().getDeclClassType());
                    declaringClassesMap.put((JavaLocal) instruction.getInvokeExpr().getUses().collect(Collectors.toList()).get(0), declaringClassList);

                } else if (!declaringClassesMap.isEmpty()) {
                    if (declaringClassesMap.containsKey(instruction.getInvokeExpr().getUses().collect(Collectors.toList()).get(0))) {
                        final List<JavaClassType> classes = declaringClassesMap.get(instruction.getInvokeExpr().getUses().collect(Collectors.toList()).get(0));
                        for (final JavaClassType classType : classes) {
                            invocation = formNewMethodSignature(instruction.getInvokeExpr().getMethodSignature(), classType);
                            checkAndFormEdge(view, invocation, method, cg);
                        }

                    }
                }
            }
            if (invocation == null) {
                continue;
            }

            checkAndFormEdge(view, invocation, method, cg);

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

    private void checkAndFormEdge(final JavaView view, final MethodSignature invocation, final MethodSignature method, final CallGraph cg) {
        final Optional<JavaSootMethod> target = view.getMethod(invocation);

        if (!cg.hasNode(method)) {
            cg.addNode(method);
        }
        if (target.isPresent() && !cg.hasNode(target.get().getSignature())) {
            cg.addNode(target.get().getSignature());
        }
        if (target.isPresent() && !cg.hasEdge(method, target.get().getSignature())) {
            target.ifPresent(javaSootMethod -> cg.addEdge(method, javaSootMethod.getSignature()));
        }
    }
}