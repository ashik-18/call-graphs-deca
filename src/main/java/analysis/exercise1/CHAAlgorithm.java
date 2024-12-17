package analysis.exercise1;

import analysis.CallGraph;
import analysis.CallGraphAlgorithm;

import java.beans.Statement;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import com.googlecode.dex2jar.ir.expr.InvokeExpr;
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
    protected void populateCallGraph(@Nonnull JavaView view, @Nonnull CallGraph cg) {
        // Get all entry points (methods considered as starting points for the analysis)
        Collection<MethodSignature> entryPoints = getEntryPoints(view).filter(e -> e.toString().contains("exercise1")).collect(Collectors.toList());

        // Iterate through each entry point and process it
        for (MethodSignature entryPoint : entryPoints) {
            processMethod(entryPoint, view, cg);
        }
    }

    private void processMethod(@Nonnull MethodSignature method, @Nonnull JavaView view, @Nonnull CallGraph cg) {
        // Get the type hierarchy from the view
        TypeHierarchy typeHierarchy = view.getTypeHierarchy();

        Optional<JavaSootMethod> javaMethodOpt = view.getMethod(method);
        if (!javaMethodOpt.isPresent()) {
            return; // Skip if method is not present
        }

        JavaSootMethod javaMethod = javaMethodOpt.get();
        if (!javaMethod.hasBody()) {
            return; // Skip methods without a body
        }

        // Analyze each instruction in the method body
        for (Stmt instruction : javaMethod.getBody().getStmts()) {
            if (instruction instanceof JInvokeStmt) {
                // Extract information about the method call
                MethodSignature invocation = instruction.getInvokeExpr().getMethodSignature();

                // Determine the possible target methods for the invocation
                Set<Optional<JavaSootMethod>> possibleTargets = resolveTargets(invocation, typeHierarchy, view, cg);

                // Add edges to the call graph
                for (Optional<JavaSootMethod> target : possibleTargets) {
                    if (!cg.hasNode(method)) {
                        cg.addNode(method);
                    }
                    if (target.isPresent() && !cg.hasNode(target.get().getSignature())) {
                        cg.addNode(target.get().getSignature());
                    }
                    if(!cg.hasEdge(method, target.get().getSignature())) {
                        target.ifPresent(javaSootMethod -> cg.addEdge(method, javaSootMethod.getSignature()));
                    }

                }
            }
        }
    }


    private Set<Optional<JavaSootMethod>> resolveTargets(@Nonnull MethodSignature invocation,
                                                         @Nonnull TypeHierarchy typeHierarchy,
                                                         @Nonnull JavaView view, @Nonnull CallGraph cg) {

        Set<Optional<JavaSootMethod>> targets = new HashSet<>();
        Set<ClassType> processedClasses = new HashSet<>();

        // Get the method signature being called
        MethodSignature signature = invocation.getDeclClassType().getStaticInitializer();

        // Get the receiver type (if applicable)
        ClassType receiverType = invocation.getDeclClassType();

        // Get all possible classes in the hierarchy that match the receiver type
        Queue<ClassType> toProcess = new LinkedList<>();
        toProcess.add(receiverType);


        while (!toProcess.isEmpty() && (!signature.toString().contains("java"))) {
            ClassType currentType = toProcess.poll();
            if (!processedClasses.add(currentType)) {
                continue; // Skip if already processed
            }

            // Check if the current type declares or inherits the target method
            Optional<JavaSootMethod> resolvedMethod = view.getMethod(invocation);

            if (!resolvedMethod.isPresent()) {
                // Traverse the superclass and interfaces to check if they declare the method
                Optional<ClassType> superClass = typeHierarchy.superClassOf(currentType);
                while (superClass.isPresent() && !superClass.get().getClassName().contains("Object")) {
                    MethodSignature updatedInvocation = withDeclClassType(invocation, superClass.get());
                    resolvedMethod = view.getMethod(updatedInvocation);
                    if (resolvedMethod.isPresent()) {
                        break;
                    }
                    superClass = typeHierarchy.superClassOf(superClass.get());
                }
            }
            if (resolvedMethod.isPresent()) {
                targets.add(resolvedMethod);
            }

            // Add subclasses and implemented interfaces for further exploration
            toProcess.addAll(typeHierarchy.subclassesOf(currentType).collect(Collectors.toSet()));
            if (typeHierarchy.isInterface(currentType)) {
                toProcess.addAll(typeHierarchy.implementersOf(currentType).collect(Collectors.toSet()));
            }


            // Add superclass for further exploration
            Optional<ClassType> superClass = typeHierarchy.superClassOf(currentType);
            superClass.ifPresent(sc -> {
                if (!sc.getClassName().contains("Object")) {
                    toProcess.add(sc);
                }
            });
        }

        if (!invocation.toString().contains("Object") && invocation.toString().contains("exercise1")) {
            if(!view.getMethod(invocation).isPresent() && !typeHierarchy.superClassesOf(receiverType).toString().contains("Object")) {
                processMethod(withDeclClassType(invocation, typeHierarchy.superClassOf(receiverType).get()), view, cg);
            }else {
                processMethod(invocation, view, cg);
            }
            processMethod(withDeclClassType(invocation, typeHierarchy.superClassOf(receiverType).get()), view, cg);
        }
        return targets;
    }


    private MethodSignature withDeclClassType(MethodSignature original, ClassType newDeclClassType) {
        return new MethodSignature(
                newDeclClassType,
                original.getName(),
                original.getParameterTypes(),
                original.getType()
        );
    }

}

