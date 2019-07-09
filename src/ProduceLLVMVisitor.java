import java.util.*;
import MethodInfo.MethodInfo;
import ClassInfo.ClassInfo;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;

public class ProduceLLVMVisitor extends GJDepthFirst<String, SymbolTable> {

    private BufferedWriter bwriter;
    
    private int tmpVarCounter;
    private int labelCounter;
    
    private Map<String, String> LLVMvarTypes;    //LLVMvarId, minijava type
	private Stack<List<String>> LLVMmethodsParamsStack; // LLVMvars used as parameters in methods call

    public ProduceLLVMVisitor(BufferedWriter bwriter)
    {
    	this.bwriter = bwriter;
        tmpVarCounter = 0;
        labelCounter = 0;
        
        this.LLVMvarTypes = new LinkedHashMap<>();
        this.LLVMmethodsParamsStack = new Stack<>();
    }
	
	//------------------LLVM Methods-------------------
	
	private void pushLLVMmethodParam(String LLVMvar)
    {
    	List<String> LLVMmethodParams = this.LLVMmethodsParamsStack.pop();
    	LLVMmethodParams.add(LLVMvar);
		this.LLVMmethodsParamsStack.push(LLVMmethodParams);
    }
    
    private String createLLVMvar()
    {
        String tmpVar = "%_var_" + tmpVarCounter;
        tmpVarCounter++;
        return tmpVar;
    }
	
	private String createLLVMvar(String name)
	{
		String tmpVar = "%_" + name + "_" + tmpVarCounter;
		tmpVarCounter++;
		return tmpVar;
	}
	
	private boolean isLLVMvar(String varId)
	{
		return varId.startsWith("%");
	}

    private String createLabel()
    {
        String label = "label_" + labelCounter;
        labelCounter++;
        return label;
    }
	
	private String createLabel(String labelName)
	{
		String label = labelName + "_" + labelCounter;
		labelCounter++;
		return label;
	}
	
	private void emit(String LLVMcode)
	{
		try
		{
			bwriter.write(LLVMcode);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private String getLLVMtype(String type)
	{
		if (type.equals("boolean"))
		{
			return "i1";
		}
		else if (type.equals("int"))
		{
			return "i32";
		}
		else if (type.equals("array"))
		{
			return "i32*";
		}
		else
		{
			return "i8*";
		}
	}
    
    private String LLVMallocate(String LLVMtype)
    {
		// %ptr = alloca i32
	    // <r> = alloca <type>
    	String r = createLLVMvar();
    	emit("\t" + r + " = alloca " + LLVMtype + "\n");
    	return r;
    }
	
    private void LLVMstore(String LLVMtype, String storingVal, String memPtr)
    {
		// store i32 %val, i32* %ptr
		// store <type> <storingVal>, <type>* memPtr
	    emit("\tstore " + LLVMtype + " " + storingVal + ", " + LLVMtype + "* " + memPtr + "\n");
    }
    
    private String LLVMload(String LLVMtype, String memPtr)
    {
		// %val = load i32, i32* %ptr
	    // <r> = load <type>, <type>* <memPtr>
	    
	    String r = createLLVMvar();
	    emit("\t" + r + " = load " + LLVMtype + ", " + LLVMtype + "* " + memPtr + "\n");
	    return r;
    }
    
    private String getLLVMphiResult(String LLVMclause1, String label1, String LLVMclause2, String label2)
    {
    	String phiResult = this.createLLVMvar();
	    emit("\t" + phiResult + " = phi i1 [" + LLVMclause1 + ", %" + label1 + "], [" + LLVMclause2 + ", %" + label2 + "]\n");
		return phiResult;
    }
	
    private String getLLVMfieldFromObj(int fieldOffset, String LLVMvarType)
    {
	    // %_1 = getelementptr i8, i8* %this, i32 24
	    // %_2 = bitcast i8* %_1 to i32*
	    
	    String fieldPtr = this.createLLVMvar();
	    emit("\t" + fieldPtr + " = getelementptr i8, i8* %this, i32 " + fieldOffset + "\n");
	
	    String castFieldPtr = this.createLLVMvar();
	    emit("\t" + castFieldPtr + " = bitcast i8* " + fieldPtr + " to " + LLVMvarType + "*\n");
     
	    return castFieldPtr;
    }
	
	private String LLVMcreateArray(String arraySize)
    {
    	String actualArraySize = this.createLLVMvar();
	    emit("\t" + actualArraySize + " = add i32 " + arraySize + ", 1\n");
    	
	    String LLVMarray = createLLVMvar();
	    emit( "\t" + LLVMarray + " = call i8* @calloc(i32 4, i32 " + actualArraySize + ")\n");
	    
	    String castLLVMarray = createLLVMvar();
	    emit("\t" + castLLVMarray + " = bitcast i8* " + LLVMarray + " to i32*\n");
	
	    this.LLVMstore("i32", arraySize, castLLVMarray);

	    return castLLVMarray;
    }
    
    private String getLLVMarrayElementPtr(String arrayPtr, String arrayIndex)
    {
    	String actualArrayIndex = this.createLLVMvar();
	    emit("\t" + actualArrayIndex + " = add i32 " + arrayIndex + ", 1\n");
	    
	    String arrayElemPtr = createLLVMvar();
	    emit("\t" + arrayElemPtr + " = getelementptr i32, i32* " + arrayPtr + ", i32 " + actualArrayIndex + "\n");
	    
	    return arrayElemPtr;
    }
    
    private String getLLVMcastedMethod(MethodInfo methodInfo, String func)
    {
	    // cast i8* to actual method type in order to call it
	    // %func = bitcast i8* %func_addr to i32 (i32*, i32*)*
	    String castFunc = createLLVMvar();
	    String castedLLVMmethod = castFunc + " = bitcast i8 * " + func + " to " + this.getLLVMtype(methodInfo.getReturnType()) + " (i8*" ;
	    for (Map.Entry<String, String> param : methodInfo.getParameters().entrySet())
	    {
		    castedLLVMmethod += ", " + this.getLLVMtype(param.getValue()) ;
	    }
	    castedLLVMmethod += ")*\n";
	    
	    emit("\t" + castedLLVMmethod);
	    return castFunc;
    }
    
    private String execLLVMmethodCall(String LLVMthis, String func, String LLVMmethodRetType, List<String> LLVMmethodParams, MethodInfo methodInfo)
    {                                                                                                           // LLVMvars
	    // call casted method
	    // %result = call i32 %func(i8* %this, i32* %ptr_a, i32* %ptr_b)
	    String result = createLLVMvar();
	    String LLVMmethodCall = result + " = call " + LLVMmethodRetType + " " + func + "(i8* " + LLVMthis ;
	    
	    for(int i=0; i < LLVMmethodParams.size(); i++)
	    {
            LLVMmethodCall += ", " + this.getLLVMtype(methodInfo.getParameterType(i)) + " " +  LLVMmethodParams.get(i);
	    }
	    LLVMmethodCall += ")\n";
	    
		emit("\t" + LLVMmethodCall);
	    return result;
    }
    
    private String callLLVMmethod(String LLVMclassObj, String classId, List<String> LLVMmethodParams, Integer methodOffset, MethodInfo methodInfo)
    {/*
    	%_3 = bitcast i8* %_0 to i8***
		%_4 = load i8**, i8*** %_3
        %_5 = getelementptr i8*, i8** %_4, i32 0
        %_6 = load i8*, i8** %_5
        %_7 = bitcast i8* %_6 to i32 (i8*,i32)*
	    %_8 = call i32 %_7(i8* %_0, i32 10) */
	    
	    emit("\n\t; " + classId + "." + methodInfo.getID() + " : " + Integer.toString(methodOffset) + "\n");
	    String castLLVMclassObj = createLLVMvar("castClassObj");
    	emit("\t" + castLLVMclassObj + " = bitcast i8* " + LLVMclassObj + " to i8***\n");
    	
    	String Vtable = LLVMload("i8**", castLLVMclassObj);
    	String funcPtr = createLLVMvar("funcPtr");
    	emit("\t" + funcPtr + " = getelementptr i8*, i8** " + Vtable + ", i32 " + Integer.toString(methodOffset / 8) + "\n");
     
    	String func = LLVMload("i8*", funcPtr);
    	String castFunc = getLLVMcastedMethod(methodInfo, func);
    	
    	String result = execLLVMmethodCall(LLVMclassObj, castFunc, getLLVMtype(methodInfo.getReturnType()), LLVMmethodParams, methodInfo);
		return result;
    }
	
	private String getLLVMmethodDecl(String methodId, MethodInfo methodInfo)
	{
		//curr_vtable + "[" + methIdSet.size() + " x i8*] [i8* bitcast (i32 (i8*,i32)* @ classId.methodId to i8*),
		String LLVMmethodDecl = "i8* bitcast (" + this.getLLVMtype(methodInfo.getReturnType()) + " (i8*";

		for (Map.Entry<String, String> param : methodInfo.getParameters().entrySet())
        {
			LLVMmethodDecl += ", " + this.getLLVMtype(param.getValue()) ;
		}

		LLVMmethodDecl += ")* @" + methodInfo.getHostClassInfo().getID() + "." + methodId + " to i8*),\n\t\t";
		
		return LLVMmethodDecl;
	}

    private void createVTable(String classId, ClassInfo classInfo)
    {
		Map<String, MethodInfo> inheritedMethodsInfo = classInfo.getInheritedMethodsInfo();

		String LLVMmethodsDecls = "";
	    for (Map.Entry<String, MethodInfo> methodInfo : inheritedMethodsInfo.entrySet())
		{
			LLVMmethodsDecls += getLLVMmethodDecl(methodInfo.getKey(), methodInfo.getValue());
		}

		if (!LLVMmethodsDecls.equals(""))
		{
			LLVMmethodsDecls = LLVMmethodsDecls.substring(0, LLVMmethodsDecls.length() - 4);
		}

		String VTable = "@." + classId + "_vtable = global [" + Integer.toString(classInfo.getVtableSize() / 8) + " x i8*] [";
		VTable += LLVMmethodsDecls + "] \n\n";
		emit(VTable);
    }
    
    private String LLVMcreateClassInstance(String classId, String classInstanceSize, String VtableSize)
    {                                                           //bytes             //cells
    	/*
        %_0 = call i8* @calloc(i32 1, i32 8)
		%_1 = bitcast i8* %_0 to i8***
		%_2 = getelementptr [1 x i8*], [1 x i8*]* @.Fac_vtable, i32 0, i32 0
	    store i8** %_2, i8*** %_1 */
    	
    	String classInstance= createLLVMvar();
    	emit( "\t" + classInstance + " = call i8* @calloc(i32 1, i32 " + classInstanceSize + ")\n");
	
		String castClassInstance= createLLVMvar();
		emit( "\t" + castClassInstance + " = bitcast i8* " + classInstance + " to i8***\n");
		
		String Vtable = createLLVMvar();
		emit("\t" + Vtable + " = getelementptr [" + VtableSize + " x i8*], [" + VtableSize + " x i8*]* @." + classId + "_vtable, i32 0, i32 0\n");
	
		LLVMstore("i8**", Vtable, castClassInstance);
		return classInstance;
    }
	
	private void emitHelpingLLVMmethods()
	{
	     emit("declare i8* @calloc(i32, i32)\n" +
			          "declare i32 @printf(i8*, ...)\n" +
			          "declare void @exit(i32)\n\n" +
			
			          "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
			          "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
			          "define void @print_int(i32 %i) {\n" +
			          "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
			          "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
			          "\tret void\n" +
			          "}\n\n" +
			
			          "define void @throw_oob() {\n" +
			          "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
			          "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
			          "\tcall void @exit(i32 1)\n" +
			          "\tret void\n" +
			          "}\n\n" );
	}
	

    //-----------------------Visitors----------------------

	
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
		
		for (Map.Entry<String, ClassInfo> classInfo : ST.getClassesInfo().entrySet())
		{
			if (classInfo.getKey() == ST.getMainClassId())
			{
				emit("@." + ST.getMainClassId() + "_vtable = global [0 x i8*] [] \n\n");
				continue;
			}
			
			classInfo.getValue().setParentsClassesInfoReversed();
			classInfo.getValue().setInheritedMethodsInfo();
			createVTable(classInfo.getKey(), classInfo.getValue());
		}
		
		emitHelpingLLVMmethods();
		
		ST.setCurrClassById(ST.getMainClassId());
		ST.setCurrMethodId("main");
		
		emit("define i32 @main() { \n");
		
		n.f14.accept(this, ST);
		n.f15.accept(this, ST);
		
		emit("\n\tret i32 0 \n}\n\n");
		
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
		String methodId = ST.extractId(n.f2.accept(this, ST));
		ST.setCurrMethodId(methodId);
		String methodRetType = ST.getCurrMethod().getReturnType();
		
		emit("define " + this.getLLVMtype(methodRetType) + " @" + ST.getCurrClass().getID() + "." + methodId + "(i8* %this");
		
		String paramLLVMallocs = "";
		for (Map.Entry<String, String> param : ST.getCurrMethod().getParameters().entrySet())
		{
			String LLVMtype = this.getLLVMtype(param.getValue());
			String LLVMparam = this.createLLVMvar();
			String LLVMparamVar = this.createLLVMvar();
			
			emit(", " + LLVMtype + " " + LLVMparam) ;
			
			paramLLVMallocs += "\t" + LLVMparamVar + " = alloca " + LLVMtype + "\n";
			paramLLVMallocs += "\t" + "store " + LLVMtype + " " + LLVMparam + ", " + LLVMtype + "* " + LLVMparamVar + "\n";
			ST.getCurrMethod().insertLLVMvar(param.getKey(), LLVMparamVar);
		}
		emit(") {\n");
		emit(paramLLVMallocs);
		
		n.f7.accept(this, ST);
		n.f8.accept(this, ST);
		
		String LLVMreturnVal = n.f10.accept(this, ST);
		emit("\n\tret " + this.getLLVMtype(methodRetType) + " " + LLVMreturnVal + "\n}\n\n");
		
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
	 * f2 -> ";"
	 */
	public String visit(VarDeclaration n, SymbolTable ST) throws Exception {
		String varType = n.f0.accept(this, ST);
		String varId = ST.extractId(n.f1.accept(this, ST));
		
		String LLVMvarId = this.LLVMallocate(this.getLLVMtype(varType));
		ST.getCurrMethod().insertLLVMvar(varId, LLVMvarId);
		
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
		String LLVMvarType = this.getLLVMtype(ST.getVarType(varId));
		
		String LLVMvarPtr;
		if (ST.getCurrMethod().LLVMvarExists(varId))
		{ //var has been declared locally inside a method
			LLVMvarPtr = ST.getCurrMethod().getLLVMvar(varId);
		}
		else
		{ // var is a class field (maybe inherited from parent class(es))
			int fieldOffset = ST.getCurrClass().getFieldOffset(varId) + 8;
			LLVMvarPtr = this.getLLVMfieldFromObj(fieldOffset, LLVMvarType);
		}
		
		String assigningLLVMvar = n.f2.accept(this, ST);
		this.LLVMstore(LLVMvarType, assigningLLVMvar, LLVMvarPtr);
		
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
		String arrayVarId = ST.extractId(n.f0.accept(this, ST));
		
		String LLVMarrayPtr;
		if (ST.getCurrMethod().LLVMvarExists(arrayVarId))
		{ //var has been declared local inside a method
			LLVMarrayPtr = ST.getCurrMethod().getLLVMvar(arrayVarId);
		}
		else
		{ // var is a class field (maybe inherited from parent class(es))
			int fieldOffset = ST.getCurrClass().getFieldOffset(arrayVarId) + 8;
			LLVMarrayPtr = this.getLLVMfieldFromObj(fieldOffset, "i32*");
		}
		
		String LLVMarray = this.LLVMload("i32*", LLVMarrayPtr);
		String LLVMarraySize = this.LLVMload("i32", LLVMarray);
		String LLVMarrayIndex = n.f2.accept(this, ST);
		
		String outOfBoundsLabel = this.createLabel("outOfBoundsLabel");
		String arrayAsgnLabel = this.createLabel("arrayAsgnLabel");
		String continueProgLabel = this.createLabel("continueProgLabel");
		
		String cmpResult = this.createLLVMvar();
		emit("\t" + cmpResult + " = icmp ult i32 " + LLVMarrayIndex + ", " + LLVMarraySize + "\n");
		emit("\tbr i1 " + cmpResult + ", label %" + arrayAsgnLabel + ", label %" + outOfBoundsLabel + "\n\n");
		
		//array index is legit
		emit(arrayAsgnLabel + ":\n");
		String LLVMassigningVar = n.f5.accept(this, ST);
		String LLVMarrayElementPtr = this.getLLVMarrayElementPtr(LLVMarray, LLVMarrayIndex);
		this.LLVMstore("i32", LLVMassigningVar, LLVMarrayElementPtr);
		emit("\tbr label %" + continueProgLabel + "\n\n");
		
		//array index is out of bounds
		emit(outOfBoundsLabel + ":\n");
		emit("\tcall void @throw_oob()\n");
		emit("\tbr label %" + continueProgLabel + "\n\n"); //command will never be executed
		
		emit(continueProgLabel + ":\n");
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
		String ifLabel = this.createLabel("ifLabel");
		String elseLabel = this.createLabel("elseLabel");
		String endIfLabel = this.createLabel("endIfLabel");
		
		String condLLVMvar = n.f2.accept(this, ST);
		emit("\tbr i1 " + condLLVMvar + ", label %" + ifLabel + ", label %" + elseLabel + "\n\n");
		
		emit(ifLabel + ":\n");
		n.f4.accept(this, ST);
		emit("\tbr label %" + endIfLabel + "\n\n");
		
		emit(elseLabel + ":\n");
		n.f6.accept(this, ST);
		emit("\tbr label %" + endIfLabel + "\n\n");
		
		emit(endIfLabel + ":\n");
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
		String loopCondLabel = this.createLabel("loopCondLabel");
		String loopStartLabel = this.createLabel("loopStartLabel");
		String loopEndLabel = this.createLabel("loopEndLabel");
		
		emit("\tbr label %" + loopCondLabel + "\n\n");
		
		emit(loopCondLabel + ":\n");
		String LLVMwhileCondVar = n.f2.accept(this, ST);
		emit("br i1 " + LLVMwhileCondVar + ", label %" + loopStartLabel + ", label %" + loopEndLabel + "\n\n");
		
		emit(loopStartLabel + ":\n");
		n.f4.accept(this, ST);
		emit("\tbr label %" + loopCondLabel + "\n\n");
		
		emit(loopEndLabel + ":\n");
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
		String printLLVMvar = n.f2.accept(this, ST);
		emit("\tcall void (i32) @print_int(i32 " + printLLVMvar + ")\n");
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
		String expr = n.f0.accept(this, ST);
		
		if (this.isLLVMvar(expr) || ST.isInteger(expr))
		{// no need to be loaded just return the LLVM variable
			return expr;
		}
		
		//so basically expr is an identifier
		String varId = ST.extractId(expr);
		String varType = ST.getVarType(varId);
		String LLVMvarType = this.getLLVMtype(varType);
		String LLVMvarPtr;
		if (ST.getCurrMethod().LLVMvarExists(varId))
		{ //var has been declared locally inside a method
			LLVMvarPtr = ST.getCurrMethod().getLLVMvar(varId);
		}
		else
		{ // var is a class field (maybe inherited from parent class(es))
			int fieldOffset = ST.getCurrClass().getFieldOffset(varId) + 8;
			LLVMvarPtr = this.getLLVMfieldFromObj(fieldOffset, LLVMvarType);
		}
		
		String LLVMvar = this.LLVMload(LLVMvarType, LLVMvarPtr);
		this.LLVMvarTypes.put(LLVMvar, varType); //map LLVM var to actual minijava type for future access

		return LLVMvar;
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
		String LLVMobjVar = n.f0.accept(this, ST);
		String classObjType;
		if (LLVMobjVar.equals("%this"))
		{
			classObjType = ST.getCurrClass().getID();
		}
		else
		{ //use mapping to gain the actual minajava class type
			classObjType = this.LLVMvarTypes.get(LLVMobjVar);
		}
		
		String methodId = ST.extractId(n.f2.accept(this, ST));
		Integer methodOffset = ST.getClassInfo(classObjType).getMethodOffset(methodId);
		MethodInfo methodInfo = ST.getClassInfo(classObjType).getInheritedMethodsInfo().get(methodId);
			
		List<String> LLVMmethodparams = new ArrayList<>();
		this.LLVMmethodsParamsStack.push(LLVMmethodparams); //using stack for nesting method calls
		n.f4.accept(this, ST);
		String LLVMcalledMethodVar = this.callLLVMmethod(LLVMobjVar, classObjType, LLVMmethodparams, methodOffset, methodInfo);
		this.LLVMmethodsParamsStack.pop();
		
		this.LLVMvarTypes.put(LLVMcalledMethodVar, methodInfo.getReturnType());
		return LLVMcalledMethodVar;
	}
	
	/**
	 * f0 -> Expression()
	 * f1 -> ExpressionTail()
	 */
	public String visit(ExpressionList n, SymbolTable ST) throws Exception {
		String LLVMvar = n.f0.accept(this, ST);
		this.pushLLVMmethodParam(LLVMvar);
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
		String LLVMvar = n.f1.accept(this, ST);
		this.pushLLVMmethodParam(LLVMvar);
		return null;
	}
	
	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	 */
	public String visit(NotExpression n, SymbolTable ST) throws Exception {
		String retLLVMvar = this.createLLVMvar();
		String LLVMclause = n.f1.accept(this, ST);
		emit("\t" + retLLVMvar + " = xor i1 1, " + LLVMclause + "\n");
		return retLLVMvar;
	}
	
	/**
	 * f0 -> Clause()
	 * f1 -> "&&"
	 * f2 -> Clause()
	 */
	public String visit(AndExpression n, SymbolTable ST) throws Exception {
		String logAndLabel = this.createLabel("logAndLabel");
		String logAndLabel2 = this.createLabel("logAndLabel2");
		String logAndLabel3 = this.createLabel("logAndLabel3");
		String logAndLabel4 = this.createLabel("logAndLabel4");
		
		String LLVMleftCond = n.f0.accept(this, ST);
		emit("\tbr label %" + logAndLabel + "\n");
		emit(logAndLabel + ":\n");
		emit("\tbr i1 " + LLVMleftCond + ", label %" + logAndLabel2 + ", label %" + logAndLabel3 + "\n");
		
		emit(logAndLabel2 + ":\n");
		String LLVMrightCond = n.f2.accept(this, ST);
		emit("\tbr label %" + logAndLabel3 + "\n");
		
		emit(logAndLabel3 + ":\n");
		emit("\tbr label %" + logAndLabel4 + "\n");
		
		emit(logAndLabel4 + ":\n");
		return this.getLLVMphiResult("0", logAndLabel, LLVMrightCond, logAndLabel3);
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(CompareExpression n, SymbolTable ST) throws Exception {
		String LLVMleftVar = n.f0.accept(this, ST);
		String LLVMrightVar = n.f2.accept(this, ST);
		
		String LLVMcmpRes = this.createLLVMvar();
		emit("\t" + LLVMcmpRes + " = icmp slt i32 " + LLVMleftVar + ", " + LLVMrightVar + "\n");
		
		return LLVMcmpRes;
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(PlusExpression n, SymbolTable ST) throws Exception {
		String LLVMleftVar = n.f0.accept(this, ST);
		String LLVMrightVar = n.f2.accept(this, ST);
		
		String LLVMplusResult = this.createLLVMvar();
		emit("\t" + LLVMplusResult + " = add i32 " + LLVMleftVar + ", " + LLVMrightVar + "\n");
		
		return LLVMplusResult;
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(MinusExpression n, SymbolTable ST) throws Exception {
		String LLVMleftVar = n.f0.accept(this, ST);
		String LLVMrightVar = n.f2.accept(this, ST);
		
		String LLVMminusResult = this.createLLVMvar();
		emit("\t" + LLVMminusResult + " = sub i32 " + LLVMleftVar + ", " + LLVMrightVar + "\n");
		
		return LLVMminusResult;
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	 */
	public String visit(TimesExpression n, SymbolTable ST) throws Exception {
		String LLVMleftVar = n.f0.accept(this, ST);
		String LLVMrightVar = n.f2.accept(this, ST);
		
		String LLVMmultResult = this.createLLVMvar();
		emit("\t" + LLVMmultResult + " = mul i32 " + LLVMleftVar + ", " + LLVMrightVar + "\n");
		
		return LLVMmultResult;
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
		return "%this";
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	 */
	public String visit(ArrayLookup n, SymbolTable ST) throws Exception {
		String LLVMarray = n.f0.accept(this, ST);
		String LLVMarraySize = this.LLVMload("i32", LLVMarray);
		
		String outOfBoundsLabel = this.createLabel("outOfBoundsLabel");
		String arrayLookupLabel = this.createLabel("arrayLookupLabel");
		String continueProgLabel = this.createLabel("continueProgLabel");
		
		String LLVMarrayIndex = n.f2.accept(this, ST);
		
		String LLVMcompResult = this.createLLVMvar();
		emit("\t" + LLVMcompResult + " = icmp ult i32 " + LLVMarrayIndex + ", " + LLVMarraySize + "\n");
		emit("\tbr i1 " + LLVMcompResult + ", label %" + arrayLookupLabel + ", label %" + outOfBoundsLabel + "\n\n");
		
		//array index is legit
		emit(arrayLookupLabel + ":\n");
		String LLVMarrayElementPtr = this.getLLVMarrayElementPtr(LLVMarray, LLVMarrayIndex);
		String LLVMarrayElement = this.LLVMload("i32", LLVMarrayElementPtr);
		emit("\tbr label %" + continueProgLabel + "\n");
		
		//array index is out of bounds
		emit(outOfBoundsLabel + ":\n");
		emit("\tcall void @throw_oob()\n");
		emit("\tbr label %" + continueProgLabel + "\n");  //will never be executed if array index is out of bounds
		
		emit(continueProgLabel + ":\n");
		return LLVMarrayElement;
	}
	
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	 */
	public String visit(ArrayLength n, SymbolTable ST) throws Exception {
		String LLVMarray = n.f0.accept(this, ST);
		return this.LLVMload("i32", LLVMarray);
	}
	
	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public String visit(ArrayAllocationExpression n, SymbolTable ST) throws Exception {
		String allocArrayLabel = this.createLabel("allocArrayLabel");
		String outOfBoundsLabel = this.createLabel("outOfBoundsLabel");
		
		String LLVMarraySizeVar = n.f3.accept(this, ST);
		String cmpResult = this.createLLVMvar();
		//checking for negative array size
		emit("\t" + cmpResult + " = icmp slt i32 " + LLVMarraySizeVar + ", 0\n");
		emit("\tbr i1 " + cmpResult + ", label %" + outOfBoundsLabel + ", label %" + allocArrayLabel + "\n\n");
		
		emit(outOfBoundsLabel + ":\n");
		emit("\tcall void @throw_oob()\n");
		emit("\tbr label %" + allocArrayLabel + "\n\n"); // will never be executed if array size is not legit
		
		//if array size is legit create a new array
		emit(allocArrayLabel + ":\n");
		String LLVMarray = this.LLVMcreateArray(LLVMarraySizeVar);
		this.LLVMvarTypes.put(LLVMarray, "array");
		return LLVMarray;
	}
	
	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	 */
	public String visit(AllocationExpression n, SymbolTable ST) throws Exception {
		String classId = ST.extractId(n.f1.accept(this, ST));
		ClassInfo classInfo = ST.getClassInfo(classId);
		String LLVMclassInstance = LLVMcreateClassInstance(classId, Integer.toString(classInfo.getFieldsSize() + 8), Integer.toString(classInfo.getVtableSize() / 8));
		this.LLVMvarTypes.put(LLVMclassInstance, classId); //mapping the LLVM var to its actual minijava type (for future lookup)
		return LLVMclassInstance;
	}
	
	/**
	 * f0 -> <INTEGER_LITERAL>
	 */
	public String visit(IntegerLiteral n, SymbolTable ST) throws Exception {
		return n.f0.toString();
	}
	
	/**
	 * f0 -> "true"
	 */
	public String visit(TrueLiteral n, SymbolTable ST) throws Exception {
		return "1";
	}
	
	/**
	 * f0 -> "false"
	 */
	public String visit(FalseLiteral n, SymbolTable ST) throws Exception {
		return "0";
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
