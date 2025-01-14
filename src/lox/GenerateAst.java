package lox;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class GenerateAst {
    public static void main(String[] args) throws IOException {
        defineAst("Expr", new String[]{
            "Grouping   : Expr expression",
            "Binary     : Expr left, Token operator, Expr right",
            "Assignment : Token name, Expr value", // we don't use Binary because `left` can't be an expression - we need a new node type in the AST
            "Unary      : Token operator, Expr right",
            "Variable   : Token name",
            "Literal    : Object value",
        });

        defineAst("Stmt", new String[]{
            "Expression : Expr expression", // an expression is also a statement (but not vice-versa)
            "Print      : Expr expression",
            // declarations
            "Var        : Token name, Expr initializer",
        });
    }    

    private static void defineAst(String baseName, String[] types) throws IOException {
        Path path = Paths.get("src/lox/" + baseName + ".java");
        
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        };
        System.out.println(baseName+".java is being generated");

        PrintWriter writer = new PrintWriter(path.toString());
        writer.println("/* CODE AUTO-GENERATED WITH GenerateAst.java */");
        writer.println("package lox;");
        writer.println();
        writer.println("abstract class "+baseName+" {");
        defineVisitor(writer, baseName, types);
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, String[] types) {
        writer.println("    interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].strip();
            writer.println("        R visit"+typeName+baseName+"("+typeName+baseName+" "+baseName.toLowerCase()+");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fields) {
        writer.println();
        writer.println("    static class "+className+baseName+" extends "+baseName+" {");
        String[] fieldsAsArr = fields.split(",");
        for (String field : fieldsAsArr) {
            writer.println("        final "+field.trim()+";");
        }
        writer.println("        "+className+baseName+"("+fields+") {");
        for (String field : fieldsAsArr) {
            String fieldName = field.trim().split(" ")[1];
            writer.println("            this."+fieldName+" = "+fieldName+";");
        }
        writer.println("        }");

        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit"+className+baseName+"(this);");
        writer.println("        }");

        writer.println("    }");
    }
}
