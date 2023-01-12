package com.craftinginterpreters.lox;
import java.util.List;
import java.util.ArrayList;

class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void>{
	final Environment globals = new Environment();
	private Environment environment = globals;

	Interpreter(){
		environment.define("clock",new LoxCallable(){
			public int arity(){
				return 0;
			}
			public Object call(Interpreter interpreter,List<Object> arguments){
				return (double)System.currentTimeMillis()/1000.0;
			}
			public String toString(){
				return "<native fn>";
			}
		
		});
		
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt){
		executeBlock(stmt.statements,new Environment(environment));
		return null;
	}
	@Override
	public Void visitContinueStmt(Stmt.Continue stmt){
		environment.setJumpPoint(TokenType.CONTINUE);
		return null;	
	}
	@Override
	public Void visitBreakStmt(Stmt.Break stmt){
		environment.setJumpPoint(TokenType.BREAK);
		return null;	
	}
	@Override
	public Void visitVarStmt(Stmt.Var stmt){
		Object value = null;
		if(stmt.initializer != null){
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexeme,value);
		return null;
	}
	@Override
	public Object visitAssignExpr(Expr.Assign expr){
		Object value = evaluate(expr.value);
		environment.assign(expr.name,value);
		return value;
	}
	@Override
	public Object visitVariableExpr(Expr.Variable expr){
		Object object = environment.get(expr.name);
		if(null == object) {
		}
		return object;
	}
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt){
		Object value = evaluate(stmt.expression);
		return null;
	}
	@Override
	public Void visitIfStmt(Stmt.If stmt){
		if(isTruthy(evaluate(stmt.condition))){
			execute(stmt.thenBranch);
		}else if(stmt.elseBranch != null){
			execute(stmt.elseBranch);
		}
		return null;
	}
	@Override
	public Object visitLogicalExpr(Expr.Logical expr){
		Object left = evaluate(expr.left);
		if(expr.operator.type == TokenType.OR){
			if(isTruthy(left)) return left;
		}else{
			if(!isTruthy(left)) return left;
		}
		return evaluate(expr.right);
	}
	@Override
	public Void visitPrintStmt(Stmt.Print stmt){
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}
	@Override
	public Void visitWhileStmt(Stmt.While stmt){
		while(isTruthy(evaluate(stmt.condition))){
			this.environment.setWhileTrue();
			execute(stmt.body);
			if(this.environment.needBreak){
				this.environment.needBreak = false;
				this.environment.needContinue =false;
				break;
			}
		}
		return null;
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr){
		return expr.value;
	}
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr){
		return evaluate(expr.expression);
	}
	@Override
	public Object visitUnaryExpr(Expr.Unary expr){
		Object right = evaluate(expr.right);
		switch(expr.operator.type){
			case MINUS:
				checkNumberOperand(expr.operator,right);
				return -(double)right;
			case BANG:
				return !isTruthy(right);
		}
		return null;
	}
	@Override
	public Object visitBinaryExpr(Expr.Binary expr){
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		switch(expr.operator.type){
			case PLUS:
				if(left instanceof Double && right instanceof Double){
					return (double)left + (double)right;
				}
				if(left instanceof String && right instanceof String){
					return (String)left + (String)right;
				}
				return left.toString()+right.toString();
			case MINUS:
				checkNumberOperands(expr.operator,left,right);
				return (double)left - (double)right;
			case SLASH:
				checkNumberOperands(expr.operator,left,right);
				return (double)left/(double)right;
			case STAR:
				return (double)left*(double)right;
			case GREATER:
				checkNumberOperands(expr.operator,left,right);
				return (double)left>(double)right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator,left,right);
				return (double)left >= (double)right;
			case LESS:
				checkNumberOperands(expr.operator,left,right);
				return (double)left<(double)right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator,left,right);
				return (double)left<=(double)right;
			case BANG_EQUAL: return !isEqual(left,right);
			case EQUAL_EQUAL: return isEqual(left,right);
		}
		return null;
	}
	@Override
	public Object visitCallExpr(Expr.Call expr){
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for(Expr argument: expr.arguments){
			arguments.add(evaluate(argument));
		}	

		if(!(callee instanceof LoxCallable)){
			throw new RuntimeError(expr.paren, "Ca only call functions or classes");
		}
		LoxCallable function = (LoxCallable)callee;
		if(arguments.size() != function.arity()){
			throw new RuntimeError(expr.paren,"Expected "+function.arity() + " arguments but got " + arguments.size()+".");
		}
		return function.call(this, arguments);
	}
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt){
		LoxFunction function = new LoxFunction(stmt);
		environment.define(stmt.name.lexeme,function);
		return null;
	}
	void executeBlock(List<Stmt> statements,Environment environment){
		Environment previous = this.environment;
		try{
			this.environment = environment;
			for(int i = 0;i<statements.size();i++){
				Stmt statement = statements.get(i);	
				execute(statement);
				if(previous.getIsWhile()){
					if(environment.needContinue){
						i = 0;
						environment.needContinue = false;
						previous.needContinue = true;
						continue;
					} 
					if(environment.needBreak){
						environment.needBreak = false;
						previous.needBreak = true;
						return;
					}
				}else{
					if(this.environment.needContinue || this.environment.needBreak){
						this.environment.needContinue = false;
						this.environment.needBreak = false;
						return;
					}
				}
			}
		}finally{
			this.environment = previous;
		}
	}
	private void checkNumberOperand(Token operator,Object operand){
		if(operand instanceof Double) return;
		throw new RuntimeError(operator,"Operand must be a number");
	}
	private void checkNumberOperands(Token operator,Object left,Object right){
		if(left instanceof Double && right instanceof Double) return;
		throw new RuntimeError(operator,"Operands must be numbers");
	}
	private boolean isEqual(Object a,Object b){
		if(a==null && b==null){
			return true;
		}
		if(a==null) return false;
		return a.equals(b);
	}
	private boolean isTruthy(Object object){
		if(object==null) return false;
		if(object instanceof Boolean) return (boolean)object;
		return true;
	}

	private Object evaluate(Expr expr){
		return expr.accept(this);
	}
	private void execute(Stmt stmt){
		stmt.accept(this);
	}
	void interpret(List<Stmt> statements){
		try{
			for(Stmt statement:statements){
				execute(statement);
			}
		}catch(RuntimeError error){
			Lox.runtimeError(error);
		}
	}
	void interpret(Expr expression){
		try{
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		}catch(RuntimeError error){
			Lox.runtimeError(error);
		}
	}
	private String stringify(Object object){
		if(object==null) return "nil";
		if(object instanceof Double){
			String text = object.toString();
			if(text.endsWith(".0")){
				text = text.substring(0,text.length()-2);
			}
			return text;
		}
		return object.toString();
	}
}
