package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment{
	private final Map<String,Object> values = new HashMap<>();
	private boolean isWhile = false;
	public boolean needContinue = false;
	public boolean needBreak = false;

	final Environment enclosing;
	Environment(){
		enclosing = null;
	}
	Environment(Environment enclosing){
		this.enclosing = enclosing;
	}
	boolean getIsWhile(){
		return this.isWhile;
	}
	void setWhileTrue(){
		this.isWhile = true;	
	}
	void setJumpPoint(TokenType type){
		boolean isNearestWhile = false; 
		Environment cur = this;
		while(!isNearestWhile){
			if(type == TokenType.CONTINUE){
				//System.out.println("setJumpPoint: cur.needContinue");
				cur.needContinue = true;
			}
			if(type == TokenType.BREAK){
				//System.out.println("setJumpPoint: cur.needBreak");
				cur.needBreak = true;		
			}
			cur = cur.enclosing;
			isNearestWhile = cur.isWhile;
			if(cur == null){
				System.out.println("cur is null");
			}
		}	
	}
	Object get(Token name){
		if(values.containsKey(name.lexeme)){
			return values.get(name.lexeme);
		}
		if(enclosing != null) return enclosing.get(name);
		throw new RuntimeError(name,"Undefined variable '"+name.lexeme+"'");
	}
	void assign(Token name,Object value){
		if(values.containsKey(name.lexeme)){
			values.put(name.lexeme,value);
			return;
		}
		if(enclosing != null){
			enclosing.assign(name,value);
			return;
		}
		throw new RuntimeError(name,"Undefined variable '"+name.lexeme+"'.");
	}
	void define(String name,Object value){
		values.put(name,value);
	}
}

