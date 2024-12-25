package analysis.exercise1;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class CHAAlgorithm extends CallGraphAlgorithm {

    @Nonnull
    @Override
    protected String getAlgorithm() {
        return "CHA";
    }

    @Override
    public void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {

        final Collection<MethodSignature> entryPoints = getEntryPoints(view).collect(Collectors.toList());

        for (MethodSignature entryPoint : entryPoints) {
            processMethod(entryPoint, view, cg);
        }
    }

    private void processMethod(final @Nonnull MethodSignature method, final @Nonnull JavaView view, final @Nonnull CallGraph cg) {

        final TypeHierarchy typeHierarchy = view.getTypeHierarchy();

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
                final MethodSignature invocation = instruction.getInvokeExpr().getMethodSignature();
                final Set<Optional<JavaSootMethod>> possibleTargets = resolveTargets(invocation, typeHierarchy, view, cg);

                // Add edges to the call graph
                for (final Optional<JavaSootMethod> target : possibleTargets) {
                    if (!cg.hasNode(method)) {
                        cg.addNode(method);
                    }
                    if (target.isPresent() && !cg.hasNode(target.get().getSignature())) {
                        cg.addNode(target.get().getSignature());
                    }
                    if (!cg.hasEdge(method, target.get().getSignature())) {
                        target.ifPresent(javaSootMethod -> cg.addEdge(method, javaSootMethod.getSignature()));
                    }
                }
            }
        }
    }

    private Set<Optional<JavaSootMethod>> resolveTargets(final @Nonnull MethodSignature invocation,
                                                         final @Nonnull TypeHierarchy typeHierarchy,
                                                         final @Nonnull JavaView view, final @Nonnull CallGraph cg) {

        final Set<Optional<JavaSootMethod>> targets = new HashSet<>();
        final Set<ClassType> processedClasses = new HashSet<>();

        final MethodSignature signature = invocation.getDeclClassType().getStaticInitializer();

        final ClassType receiverType = invocation.getDeclClassType();

        // Get all possible classes in the hierarchy that match the receiver type
        final Queue<ClassType> toProcess = new LinkedList<>();
        toProcess.add(receiverType);

        while (!toProcess.isEmpty() && (!signature.toString().contains("java"))) {
            ClassType currentType = toProcess.poll();
            if (!processedClasses.add(currentType)) {
                continue;
            }

            // Check if the current type declares or inherits the target method
            final List<Optional<JavaSootMethod>> resolvedMethods = new ArrayList<>(Collections.singletonList(view.getMethod(invocation)));

            if (!typeHierarchy.superClassesOf(receiverType).toString().contains("Object")) {
                resolvedMethods.add(view.getMethod(withDeclClassType(invocation, typeHierarchy.superClassOf(receiverType).get())));
            }

            if (typeHierarchy.isInterface(currentType)) {
                resolvedMethods.add(view.getMethod(withDeclClassType(invocation, typeHierarchy.implementersOf(receiverType).findAny().get())));
            }

//            if (!typeHierarchy.subclassesOf(receiverType).collect(Collectors.toList()).isEmpty() && !typeHierarchy.subclassesOf(receiverType).toString().contains("Object")) {
//                resolvedMethods.add(view.getMethod(withDeclClassType(invocation, typeHierarchy.subclassesOf(receiverType).findAny().get())));
//            }

            if (!resolvedMethods.isEmpty() && resolvedMethods.stream().allMatch(Optional::isPresent)) {
                // Traverse the superclass and interfaces to check if they declare the method
                Optional<ClassType> superClass = typeHierarchy.superClassOf(currentType);
                while (superClass.isPresent() && !superClass.get().getClassName().equals("Object")) {
                    MethodSignature updatedInvocation = withDeclClassType(invocation, superClass.get());
                    resolvedMethods.add(view.getMethod(updatedInvocation));
                    if (resolvedMethods.isEmpty()) {
                        break;
                    }
                    superClass = typeHierarchy.superClassOf(superClass.get());
                }
            }
            if (!resolvedMethods.isEmpty()) {
                resolvedMethods.forEach(rm -> {
                    if (rm.isPresent()) {
                        targets.add(rm);
                    }
                });
            }

            // Add subclasses and implemented interfaces if present
            if (typeHierarchy.isInterface(currentType)) {
                toProcess.addAll(typeHierarchy.implementersOf(currentType).collect(Collectors.toSet()));
            } else {
                toProcess.addAll(typeHierarchy.subclassesOf(currentType).collect(Collectors.toSet()));
            }

            final Optional<ClassType> superClass = typeHierarchy.superClassOf(currentType);
            superClass.ifPresent(sc -> {
                if (!sc.getClassName().equals("Object")) {
                    toProcess.add(sc);
                }
            });
        }

        // for the methods inside the current method
        if (!invocation.getDeclClassType().toString().contains("Object") && invocation.toString().contains("exercise1")) {
            if (!view.getMethod(invocation).isPresent() && !typeHierarchy.superClassesOf(receiverType).toString().contains("Object")) {
                processMethod(withDeclClassType(invocation, typeHierarchy.superClassOf(receiverType).get()), view, cg);
            } else {
                processMethod(invocation, view, cg);
            }
            processMethod(withDeclClassType(invocation, typeHierarchy.superClassOf(receiverType).get()), view, cg);
        }
        return targets;
    }

    private MethodSignature withDeclClassType(final MethodSignature original, final ClassType newDeclClassType) {
        return new MethodSignature(
                newDeclClassType,
                original.getName(),
                original.getParameterTypes(),
                original.getType()
        );
    }

}