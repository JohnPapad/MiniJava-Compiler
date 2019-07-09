import MethodInfo.MethodInfo;
import ClassInfo.ClassInfo;
import syntaxtree.*;
import visitor.GJDepthFirst;

public class TypeCheckingVisitor extends GJDepthFirst<String, SymbolTable> {
	
	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> "public"
	 * f4 -> "static"
	 * f5 -> "void"
	 * f6 -> "main"
	 * f7 -> "("
	 * f8 -> "String"
	 * f9 -> "["
	 * f10 -> "]"
	 * f11 -> Identifier()
	 * f12 -> ")"
	 * f13 -> "{"
	 * f14 -> ( VarDeclaration() )*
	 * f15 -> ( Statement() )*
	 * f16 -> "}"
	 * f17 -> "}"
	 */
	public String visit(MainClass n, SymbolTable ST) throws Exception {
		String classId = ST.extractId(n.f1.accept(this, ST));
		ST.setCurrClassById(classId);
		ST.setCurrMethodId("main");
		
		n.f14.accept(this, ST);
		n.f15.accept(this, ST);
		
		ST.setCurrMethodId(null);
		ST.setCurrClassById(null);
		
		return null;
	}
	
	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	 */
	public String visit(ClassDeclaration n, SymbolTable ST) throws Exception {
		String classId = ST.extractId(n.f1.accept(this, ST));
		ST.setCurrClassById(classId);
		
		n.f3.accept(this, ST);
		n.f4.accept(this, ST);
		
		ST.setCurrClassById(null);
		
		return null;
	}
	
	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "extends"
	 * f3 -> Identifier()
	 * f4 -> "{"
	 * f5 -> ( VarDeclaration() )*
	 * f6 -> ( MethodDeclaration() )*
	 * f7 -> "}"
	 */
	public String visit(ClassExtendsDeclaration n, SymbolTable ST) throws Exception {
		String classId = ST.extractId(n.f1.accept(this, ST));
		ST.setCurrClassById(classId);
		
		n.f5.accept(this, ST);
		n.f6.accept(this, ST);
		
		ST.setCurrClassById(null);
		
		return null;
	}
	
	/**
	 * f0 -> "public"
	 * f1 -> Type()
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( FormalParameterList() )?
	 * f5 -> ")"
	 * f6 -> "{"
	 * f7 -> ( VarDeclaration() )*
	 * f8 -> ( Statement() )*
	 * f9 -> "return"
	 * f10 -> Expression()
	 * f11 -> ";"
	 * f12 -> "}"
	 */
	public String visit(MethodDeclaration n, SymbolTable ST) throws Exception {
		String methodRetType = n.f1.accept(this, ST);
		if (!ST.isPrimitiveType(methodRetType) && !ST.classExists(methodRetType))
		{
			throw new Exception("-> Error at line #" + n.f0.beginLine + ": the function's return type '" + methodRetType +
					                    "' does not exist");
		}
		
		String methodId = ST.extractId(n.f2.accept(this, ST));
		ST.setCurrMethodId(methodId);
		
		n.f4.accept(this, ST);
		n.f7.accept(this, ST);
		n.f8.accept(this, ST);
		
		String returnType;
		String returnExpr = n.f10.accept(this, ST);
		if (ST.isID(returnExpr))
		{
			String varId = ST.extractId(returnExpr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f9.beginLine + ": the returning variable '" +
						                    varId + "' has not been declared previously");
			}
			returnType = varType;
		}
		else
		{
			returnType = returnExpr;
		}
		
		int res = ST.checkAssignmentLegitimacy(methodRetType, returnType);
		
		if (res == 0)
		{
			throw new Exception("-> Error at line #" + n.f11.beginLine + ": the returning type '" + returnType +
					                    "' is different from the function's declared return type '" + methodRetType + "'");
		}
		else if (res == -1)
		{
			throw new Exception("-> Error at line #" + n.f11.beginLine + ": the returning type is of class '" +
					                    returnType + "' which is not compatible with the function's declared return type '"
					                    + methodRetType + "'");
		}
		
		ST.setCurrMethodId(null);
		
		return null;
	}
	
	/**
	 * f0 -> Block()
	 *       | AssignmentStatement()
	 *       | ArrayAssignmentStatement()
	 *       | IfStatement()
	 *       | WhileStatement()
	 *       | PrintStatement()
	 */
	public String visit(Statement n, SymbolTable ST) throws Exception {
		return n.f0.accept(this, ST);
	}
	
	/**
	 * f0 -> "{"
	 * f1 -> ( Statement() )*
	 * f2 -> "}"
	 */
	public String visit(Block n, SymbolTable ST) throws Exception {
		return n.f1.accept(this, ST);
	}
	
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 */
	public String visit(FormalParameter n, SymbolTable ST) throws Exception {
		String paramType = n.f0.accept(this, ST);
		if (!ST.isPrimitiveType(paramType) && !ST.classExists(paramType))
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": parameter's type of class '" + paramType +
					                    "' does not exist");
		}
		
		return null;
	}
	
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	public String visit(VarDeclaration n, SymbolTable ST) throws Exception {
		String varType = n.f0.accept(this, ST);
		if (!ST.isPrimitiveType(varType) && !ST.classExists(varType))
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": variable's type of class '" + varType +
					                    "' does not exist");
		}
		
		return null;
	}
	
	/**
	 * f0 -> Identifier()
	 * f1 -> "="
	 * f2 -> Expression()
	 * f3 -> ";"
	 */
	public String visit(AssignmentStatement n, SymbolTable ST) throws Exception {
		String varId = ST.extractId(n.f0.accept(this, ST));
		String varType = ST.getVarType(varId);
		if (varType == null)
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" + varId + "' has not been declared previously");
		}
		
		String assigningType;
		String assigningExpr = n.f2.accept(this, ST);
		if (ST.isID(assigningExpr))
		{
			String asgnVarId = ST.extractId(assigningExpr);
			String asgnVarType = ST.getVarType(asgnVarId);
			if (asgnVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": the assigning variable '" +
						                    asgnVarId + "' has not been declared previously");
			}
			assigningType = asgnVarType;
		}
		else
		{
			assigningType = assigningExpr;
		}
		
		int res = ST.checkAssignmentLegitimacy(varType, assigningType);
		
		if (res == 0)
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": the assigning type '" + assigningType +
					                    "' is different from the variable's type '" + varType + "' to be assigned");
		}
		else if (res == -1)
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": assigning type is of class '" +
					                    assigningType + "' which is not compatible with the variable's type '" +
					                    varType + "' to be assigned");
		}
		
		return null;
	}
	
	/**
	 * f0 -> Identifier()
	 * f1 -> "["
	 * f2 -> Expression()
	 * f3 -> "]"
	 * f4 -> "="
	 * f5 -> Expression()
	 * f6 -> ";"
	 */
	public String visit(ArrayAssignmentStatement n, SymbolTable ST) throws Exception {
		String varId = ST.extractId(n.f0.accept(this, ST));
		String varType = ST.getVarType(varId);
		
		if (varType == null)
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" + varId + "' has not been declared previously");
		}
		
		if (!varType.equals("array"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable's type must be an array");
		}
		
		String indexExprType;
		String indexExpr = n.f2.accept(this, ST);
		if (ST.isID(indexExpr))
		{
			String indexVarId = ST.extractId(indexExpr);
			String indexVarType = ST.getVarType(indexVarId);
			if (indexVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    indexVarId + "' has not been declared previously");
			}
			indexExprType = indexVarType;
		}
		else
		{
			indexExprType = indexExpr;
		}
		
		if (!indexExprType.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f3.beginLine + ": array's index must be an integer");
		}
		
		String assigningType;
		String assigningExpr = n.f5.accept(this, ST);
		if (ST.isID(assigningExpr))
		{
			String asgnVarId = ST.extractId(assigningExpr);
			String asgnVarType = ST.getVarType(asgnVarId);
			if (asgnVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f4.beginLine + ": the assigning variable '" +
						                    asgnVarId + "' has not been declared previously");
			}
			assigningType = asgnVarType;
		}
		else
		{
			assigningType = assigningExpr;
		}
		
		if (!assigningType.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f4.beginLine + ": cannot allocate '" + assigningType + "' to an integer array");
		}
		
		return null;
	}
	
	/**
	 * f0 -> "if"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 * f5 -> "else"
	 * f6 -> Statement()
	 */
	public String visit(IfStatement n, SymbolTable ST) throws Exception {
		String condExprType;
		String condExpr = n.f2.accept(this, ST);
		if (ST.isID(condExpr))
		{
			String condVarId = ST.extractId(condExpr);
			String condVarType = ST.getVarType(condVarId);
			if (condVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f3.beginLine + ": variable '" +
						                    condVarId + "' has not been declared previously");
			}
			condExprType = condVarType;
		}
		else
		{
			condExprType = condExpr;
		}
		
		if (!condExprType.equals("boolean"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": if statement's condition must be boolean");
		}
		
		n.f4.accept(this, ST);
		n.f6.accept(this, ST);
		
		return null;
	}
	
	/**
	 * f0 -> "while"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 */
	public String visit(WhileStatement n, SymbolTable ST) throws Exception {
		String whileExprType;
		String whileExpr = n.f2.accept(this, ST);
		if (ST.isID(whileExpr))
		{
			String whileVarId = ST.extractId(whileExpr);
			String whileVarType = ST.getVarType(whileVarId);
			if (whileVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    whileVarId + "' has not been declared previously");
			}
			whileExprType = whileVarType;
		}
		else
		{
			whileExprType = whileExpr;
		}
		
		
		if (!whileExprType.equals("boolean"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": while statement's condition must be boolean");
		}
		
		n.f4.accept(this, ST);
		return null;
	}
	
	/**
	 * f0 -> "System.out.println"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> ";"
	 */
	public String visit(PrintStatement n, SymbolTable ST) throws Exception {
		String printExprType;
		String printExpr = n.f2.accept(this, ST);
		if (ST.isID(printExpr))
		{
			String printVarId = ST.extractId(printExpr);
			String printVarType = ST.getVarType(printVarId);
			if (printVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    printVarId + "' has not been declared previously");
			}
			printExprType = printVarType;
		}
		else
		{
			printExprType = printExpr;
		}
		
		if (!ST.isPrimitiveType(printExprType))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": cannot print class type variables");
		}
		else if (printExprType.equals("array"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": cannot print array type variables");
		}
		
		return null;
	}
	
	/**
	 * f0 -> AndExpression()
	 *       | CompareExpression()
	 *       | PlusExpression()
	 *       | MinusExpression()
	 *       | TimesExpression()
	 *       | ArrayLookup()
	 *       | ArrayLength()
	 *       | MessageSend()
	 *       | Clause()
	 */
	public String visit(Expression n, SymbolTable ST) throws Exception {
		return n.f0.accept(this, ST);
	}
	
	/**
	 * f0 -> IntegerLiteral()
	 *       | TrueLiteral()
	 *       | FalseLiteral()
	 *       | Identifier()
	 *       | ThisExpression()
	 *       | ArrayAllocationExpression()
	 *       | AllocationExpression()
	 *       | BracketExpression()
	 */
	public String visit(PrimaryExpression n, SymbolTable ST) throws Exception {
		return n.f0.accept(this, ST);
	}
	
	/**
	 * f0 -> NotExpression()
	 *       | PrimaryExpression()
	 */
	public String visit(Clause n, SymbolTable ST) throws Exception {
		return n.f0.accept(this, ST);
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( ExpressionList() )?
	 * f5 -> ")"
	 */
	public String visit(MessageSend n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String objectId = ST.extractId(expr);
			String objectType = ST.getVarType(objectId);
			if (objectType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    objectId + "' has not been declared previously");
			}
			exprType = objectType;
		}
		else
		{
			exprType = expr;
		}
		
		if (ST.isPrimitiveType(exprType) || (exprType.equals("StringArray")))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": cannot call method at non class object");
		}
		
		String methodId = ST.extractId(n.f2.accept(this, ST));
		MethodInfo currCheckingMethod;
		ClassInfo currCheckingClass = ST.getClassInfo(exprType);
		if (currCheckingClass.methodExists(methodId))
		{
			currCheckingMethod = currCheckingClass.getMethod(methodId);
		}
		else
		{
			currCheckingMethod = currCheckingClass.getMethodInfoFromParent(methodId);
		}
		
		if (currCheckingMethod == null)
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": class '" + exprType + "' does not contain method '"
					                    + methodId + "'");
		}
		
		String currCheckingMethodRetType = currCheckingMethod.getReturnType();
		if (!ST.isPrimitiveType(currCheckingMethodRetType) && !ST.classExists(currCheckingMethodRetType))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": method's '" + methodId + "' declared return type '"
					                    + currCheckingMethodRetType + "' does not exist");
		}
		
		ST.pushMethodInfoCall(currCheckingMethod);
		ST.pushMethodArgIndex();
		
		ST.setCurrLineNum(n.f3.beginLine);
		n.f4.accept(this, ST);
		if (currCheckingMethod.getParametersNumber() != ST.getMethodArgIndex())
		{
			throw new Exception("-> Error at line #" + n.f3.beginLine + ": wrong argument number for method '" + methodId +
					                    "' call");
		}
		
		ST.popMethodInfoCall();
		ST.popMethodArgIndex();

		return currCheckingMethodRetType;
	}
	
	/**
	 * f0 -> Expression()
	 * f1 -> ExpressionTail()
	 */
	public String visit(ExpressionList n, SymbolTable ST) throws Exception {
		String chkMethodFirstArgType = ST.getMethodInfoCall().getParameterType(0);
		if (chkMethodFirstArgType == null)
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": wrong argument number for method '"
					                    + ST.getMethodInfoCall().getID() + "' call");
		}
		
		String methodArgExprType;
		String methodArgExpr = n.f0.accept(this, ST);
		if (ST.isID(methodArgExpr))
		{
			String methodArgId = ST.extractId(methodArgExpr);
			String methodArgType = ST.getVarType(methodArgId);
			if (methodArgType == null)
			{
				throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": variable '" +
						                    methodArgId + "' has not been declared previously");
			}
			methodArgExprType = methodArgType;
		}
		else
		{
			methodArgExprType = methodArgExpr;
		}
		
		int res = ST.checkAssignmentLegitimacy(chkMethodFirstArgType, methodArgExprType);
		
		if (res == 0)
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": passing argument #1 on method '" +
					                    ST.getMethodInfoCall().getID() + "' call is of type '" + methodArgExprType +
					                    "' but has been declared as '" + chkMethodFirstArgType + "'");
		}
		else if (res == -1)
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": passing argument #1 on method '" +
					                    ST.getMethodInfoCall().getID() + "' call is of class type '" + methodArgExprType +
					                    "' which is not compatible with the declared's type '" + chkMethodFirstArgType + "'");
		}
		
		ST.increaseMethodArgIndex();
		n.f1.accept(this, ST);
		
		return null;
	}
	
	/**
	 * f0 -> ( ExpressionTerm() )*
	 */
	public String visit(ExpressionTail n, SymbolTable ST) throws Exception {
		return n.f0.accept(this, ST);
	}
	
	/**
	 * f0 -> ","
	 * f1 -> Expression()
	 */
	public String  visit(ExpressionTerm n, SymbolTable ST) throws Exception {
		Integer methodArgIndex = ST.getMethodArgIndex();
		String chkMethodArgType = ST.getMethodInfoCall().getParameterType(methodArgIndex);
		if (chkMethodArgType == null)
		{
			throw new Exception("-> Error at line #" + n.f0.beginLine + ": wrong argument number for method '"
					                    + ST.getMethodInfoCall().getID() + "' call");
		}
		
		String methodArgExprType;
		String methodArgExpr = n.f1.accept(this, ST);
		if (ST.isID(methodArgExpr))
		{
			String methodArgId = ST.extractId(methodArgExpr);
			String methodArgType = ST.getVarType(methodArgId);
			if (methodArgType == null)
			{
				throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": variable '" +
						                    methodArgId + "' has not been declared previously");
			}
			methodArgExprType = methodArgType;
		}
		else
		{
			methodArgExprType = methodArgExpr;
		}
		
		int res = ST.checkAssignmentLegitimacy(chkMethodArgType, methodArgExprType);
		
		if (res == 0)
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": passing argument #" + (methodArgIndex + 1) +
					                    " on method '" + ST.getMethodInfoCall().getID() + "' call is of type '" + methodArgExprType +
					                    "' but has been declared as '" + chkMethodArgType + "'");
		}
		else if (res == -1)
		{
			throw new Exception("-> Error at line #" + ST.getCurrLineNum() + ": passing argument #" + (methodArgIndex + 1) +
					                    " on method '" + ST.getMethodInfoCall().getID() + "' call is of class type '" + methodArgExprType +
					                    "' which is not compatible with the declared's type '" + chkMethodArgType + "'");
		}
		
		ST.increaseMethodArgIndex();
		
		return null;
	}
	
	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	 */
	public String visit(NotExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f1.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f0.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		if (!exprType.equals("boolean"))
		{
			throw new Exception("-> Error at line #" + n.f0.beginLine + ": logical not operator cannot be applied to '" +
					                    exprType + "' type");
		}
		
		return "boolean";
	}
	
	/**
	 * f0 -> Clause()
	 * f1 -> "&&"
	 * f2 -> Clause()
	 */
	public String visit(AndExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		
		String exprType2;
		String expr2 = n.f2.accept(this, ST);
		if (ST.isID(expr2))
		{
			String varId = ST.extractId(expr2);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType2 = varType;
		}
		else
		{
			exprType2 = expr2;
		}
		
		if (!exprType.equals("boolean") || !exprType2.equals("boolean"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": logical and operator can only be applied to booleans");
		}
		
		return "boolean";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(CompareExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		
		String exprType2;
		String expr2 = n.f2.accept(this, ST);
		if (ST.isID(expr2))
		{
			String varId = ST.extractId(expr2);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType2 = varType;
		}
		else
		{
			exprType2 = expr2;
		}
		
		if (!exprType.equals("int") || !exprType2.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": compare expression must be composed of integers");
		}
		
		return "boolean";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(PlusExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		
		String exprType2;
		String expr2 = n.f2.accept(this, ST);
		if (ST.isID(expr2))
		{
			String varId = ST.extractId(expr2);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType2 = varType;
		}
		else
		{
			exprType2 = expr2;
		}
		
		if (!exprType.equals("int") || !exprType2.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": plus expression must be composed of integers");
		}
		
		return "int";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(MinusExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		
		String exprType2;
		String expr2 = n.f2.accept(this, ST);
		if (ST.isID(expr2))
		{
			String varId = ST.extractId(expr2);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType2 = varType;
		}
		else
		{
			exprType2 = expr2;
		}
		
		if (!exprType.equals("int") || !exprType2.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": minus expression must be composed of integers");
		}
		
		return "int";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(TimesExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		
		String exprType2;
		String expr2 = n.f2.accept(this, ST);
		if (ST.isID(expr2))
		{
			String varId = ST.extractId(expr2);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType2 = varType;
		}
		else
		{
			exprType2 = expr2;
		}
		
		if (!exprType.equals("int") || !exprType2.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": times expression must be composed of integers");
		}
		
		return "int";
	}
	
	/**
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	 */
	public String visit(BracketExpression n, SymbolTable ST) throws Exception {
		return n.f1.accept(this, ST);
	}
	
	
	/**
	 * f0 -> "this"
	 */
	public String visit(ThisExpression n, SymbolTable ST) throws Exception {
		return ST.getCurrClass().getID();
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	 */
	public String visit(ArrayLookup n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		if (!exprType.equals("array"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": attempting array lookup to non array variable");
		}
		
		String indexExprType;
		String indexExpr = n.f2.accept(this, ST);
		if (ST.isID(indexExpr))
		{
			String indexVarId = ST.extractId(indexExpr);
			String indexVarType = ST.getVarType(indexVarId);
			if (indexVarType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    indexVarId + "' has not been declared previously");
			}
			indexExprType = indexVarType;
		}
		else
		{
			indexExprType = indexExpr;
		}
		
		if (!indexExprType.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": array index must be integer");
		}
		
		return "int";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	 */
	public String visit(ArrayLength n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f0.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		if (!exprType.equals("array"))
		{
			throw new Exception("-> Error at line #" + n.f1.beginLine + ": attempting getting the length of a non array variable");
		}
		
		return "int";
	}
	
	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public String visit(ArrayAllocationExpression n, SymbolTable ST) throws Exception {
		String exprType;
		String expr = n.f3.accept(this, ST);
		if (ST.isID(expr))
		{
			String varId = ST.extractId(expr);
			String varType = ST.getVarType(varId);
			if (varType == null)
			{
				throw new Exception("-> Error at line #" + n.f1.beginLine + ": variable '" +
						                    varId + "' has not been declared previously");
			}
			exprType = varType;
		}
		else
		{
			exprType = expr;
		}
		
		if (!exprType.equals("int"))
		{
			throw new Exception("-> Error at line #" + n.f2.beginLine + ": the size allocator" +
					                    " of an array must be an integer");
		}
		
		return "array";
	}
	
	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	 */
	public String visit(AllocationExpression n, SymbolTable ST) throws Exception {
		String classId = ST.extractId(n.f1.accept(this, ST));
		if (!ST.classExists(classId))
		{
			throw new Exception("-> Error at line #" + n.f0.beginLine + ": cannot create instance of class '" +
					                    classId + "'. Class declaration does not exist");
		}
		
		if (classId.equals(ST.getMainClassId()))
		{
			throw new Exception("-> Error at line #" + n.f0.beginLine + ": cannot create instance of Main Class '" +
					                    ST.getMainClassId() + "'");
		}
		
		return classId;
	}
	
	/**
	 * f0 -> <INTEGER_LITERAL>
	 */
	public String visit(IntegerLiteral n, SymbolTable ST) throws Exception {
		return "int";
	}
	
	/**
	 * f0 -> "true"
	 */
	public String visit(TrueLiteral n, SymbolTable ST) throws Exception {
		return "boolean";
	}
	
	/**
	 * f0 -> "false"
	 */
	public String visit(FalseLiteral n, SymbolTable ST) throws Exception {
		return "boolean";
	}
	
	/**
	 * f0 -> ArrayType()
	 *       | BooleanType()
	 *       | IntegerType()
	 *       | Identifier()
	 */
	public String visit(Type n, SymbolTable ST) throws Exception {
		String type = n.f0.accept(this, ST);
		if (!ST.isPrimitiveType(type))
		{
			return ST.extractId(type);
		}
		else
		{
			return type;
		}
	}
	
	/**
	 * f0 -> "int"
	 * f1 -> "["
	 * f2 -> "]"
	 */
	public String visit(ArrayType n, SymbolTable ST) throws Exception {
		return "array";
	}
	
	/**
	 * f0 -> "boolean"
	 */
	public String visit(BooleanType n, SymbolTable ST) throws Exception {
		return "boolean";
	}
	
	/**
	 * f0 -> "int"
	 */
	public String visit(IntegerType n, SymbolTable ST) throws Exception {
		return "int";
	}
	
	/**
	 * f0 -> <IDENTIFIER>
	 */
	public String visit(Identifier n, SymbolTable ST) throws Exception {
		ST.setCurrLineNum(n.f0.beginLine);
		return "@" + n.f0.toString();
	}

}
