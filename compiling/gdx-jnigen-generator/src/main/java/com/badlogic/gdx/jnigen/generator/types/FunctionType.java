package com.badlogic.gdx.jnigen.generator.types;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.HashMap;

public class FunctionType {

    private final FunctionSignature signature;
    private final String comment;

    public FunctionType(FunctionSignature signature, String comment) {
        this.signature = signature;
        this.comment = comment;
    }

    public FunctionSignature getSignature() {
        return signature;
    }

    public void write(CompilationUnit cu, ClassOrInterfaceDeclaration wrappingClass, HashMap<MethodDeclaration, String> patchNativeMethod) {
        String name = signature.getName();
        TypeDefinition returnType = signature.getReturnType();
        NamedType[] arguments = signature.getArguments();

        MethodDeclaration callMethod = wrappingClass.addMethod(name, Keyword.PUBLIC, Keyword.STATIC);
        if (comment != null)
            callMethod.setJavadocComment(comment);
        BlockStmt body = new BlockStmt();

        MethodDeclaration nativeMethod = wrappingClass.addMethod(name + "_internal").setStatic(true).setPrivate(true).setNative(true).setBody(null);
        callMethod.setType(returnType.getMappedType().abstractType());
        returnType.getMappedType().importType(cu);
        nativeMethod.setType(returnType.getMappedType().primitiveType());

        MethodCallExpr callExpr = new MethodCallExpr(nativeMethod.getNameAsString());

        StringBuilder nativeBody = new StringBuilder();
        nativeBody.append(name).append("(");
        for (NamedType namedType : arguments) {
            namedType.getDefinition().getMappedType().importType(cu);
            callMethod.addParameter(namedType.getDefinition().getMappedType().abstractType(), namedType.getName());
            if (namedType.getDefinition().getTypeKind() == TypeKind.STACK_ELEMENT) {
                nativeBody.append("*(").append(namedType.getDefinition().getTypeName()).append("*)").append(namedType.getName()).append(", ");
            } else {
                nativeBody.append("(").append(namedType.getDefinition().getTypeName()).append(")").append(namedType.getName()).append(", ");
            }
            nativeMethod.addParameter(namedType.getDefinition().getMappedType().primitiveType(), namedType.getName());

            Statement assertStatement = namedType.getDefinition().getMappedType().assertJava(new NameExpr(namedType.getName()));
            if (!assertStatement.isEmptyStmt())
                body.addStatement(assertStatement);
            callExpr.addArgument(namedType.getDefinition().getMappedType().toC(new NameExpr(namedType.getName())));
        }
        if (arguments.length != 0)
            nativeBody.delete(nativeBody.length() - 2, nativeBody.length());
        nativeBody.append(");");

        if (returnType.getTypeKind() != TypeKind.VOID) {
            if (returnType.getTypeKind().isPrimitive()) {
                nativeBody.insert(0, "return (j" + returnType.getMappedType().primitiveType() + ")");
                body.addStatement(new ReturnStmt(callExpr));
            } else {
                if (returnType.getTypeKind() == TypeKind.STACK_ELEMENT) {
                    nativeBody.insert(0, "*_ret = ");
                    nativeBody.insert(0, returnType.getTypeName() + "* _ret = (" + returnType.getTypeName() + "*)malloc(sizeof(" + returnType.getTypeName() + "));\n");
                    nativeBody.append("\nreturn (jlong)_ret;");
                } else {
                    nativeBody.insert(0, "return (j" + returnType.getMappedType().primitiveType() + ")");
                }
                body.addStatement(new ReturnStmt(returnType.getMappedType().fromC(callExpr)));
            }
        } else {
            body.addStatement(callExpr);
        }
        callMethod.setBody(body);

        for (int i = 0; i < arguments.length; i++) {
            NamedType namedType = arguments[i];
            TypeKind typeKind = namedType.getDefinition().getTypeKind();
            if (typeKind.isPrimitive() && typeKind != TypeKind.FLOAT && typeKind != TypeKind.DOUBLE) {
                String ret = returnType.getTypeKind() == TypeKind.VOID ? "return" : "return 0";
                nativeBody.insert(0, "CHECK_AND_THROW_C_TYPE(env, " + namedType.getDefinition().getTypeName() + ", "
                        + namedType.getName() + ", " + i + ", " + ret + ");\n");
            }
        }

        nativeBody.insert(0, "HANDLE_JAVA_EXCEPTION_START()\n");
        nativeBody.append("\nHANDLE_JAVA_EXCEPTION_END()");
        if (returnType.getTypeKind() != TypeKind.VOID)
            nativeBody.append("\nreturn 0;");

        patchNativeMethod.put(nativeMethod, nativeBody.toString());
    }
}
