package com.craftinginterpreters.tool;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

public class GenerateStmt{
	public static void main(String[] args) throws IOException{
		if(args.length!=1){
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		defineAst(outputDir,"Stmt",Arrays.asList(
			"Block : List<Stmt> statements",
			"Expression: Expr expression",
			"If: Expr condition, Stmt thenBranch, Stmt elseBranch",
			"Print: Expr expression",
			"Var: Token name,Expr initializer",
			"While: Expr condition,Stmt body",
			"Continue: ",
			"Break: ",
			"Function : Token name, List<Token> params, List<Stmt> body"	
		));
	}
	private static void defineAst(String outputDir,String baseName,List<String> types) throws IOException{
		String path = outputDir +"/"+baseName+".java";
		PrintWriter writer = new PrintWriter(path,"UTF-8");
		writer.println("package com.craftinginterpreters.lox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println("abstract class " + baseName + "{");
		
		defineVisitor(writer,baseName,types);
		writer.println();
		writer.println("	abstract <R> R accept(Visitor<R> visitor);");

		for(String type:types){
			writer.println();
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer,baseName,className,fields);
			writer.println();
		}

		writer.println("}");
		writer.close();
	}

	private static void defineVisitor(PrintWriter writer,String baseName,List<String> types){
		writer.println("	interface Visitor<R>{");
		for (String type:types){
			String typeName = type.split(":")[0].trim();
			writer.println("		R visit" + typeName+baseName+"("+typeName+" "+baseName.toLowerCase()+");");
		}	
		writer.println("	}");
	}

	private static void defineType(PrintWriter writer,String baseName,String className,String fieldsList){
		writer.println("	static class " + className + " extends "+baseName + " {");
		writer.println("		"+className+"("+fieldsList+"){");
		String[] fields = fieldsList.split(",");
		for (String field:fields){
			if(field.trim().equals("")){
				continue;
			}
			String name = field.trim().split(" ")[1].trim();
			writer.println("			this."+name+" = "+name+";");
		}
		writer.println("		}");
		writer.println();
		writer.println("		@Override");
		writer.println("		<R> R accept(Visitor<R> visitor){");
		writer.println("			return visitor.visit"+className+baseName+"(this);");
		writer.println("		}");
		for(String field:fields){
			if(field.trim().equals("")){
				continue;
			}
			writer.println("		final "+field+";");
		}
		writer.println("	}");
	}
}
