
import syntaxtree.*;
import visitor.GJDepthFirst;

public class FillSTVisitor extends GJDepthFirst<String, SymbolTable> {

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
        String classId = n.f1.accept(this, ST);
        ST.insertClass(classId, null);
	    ST.setMainClassId(classId);
	
        ST.getCurrClass().insertMethod("main", "void");
        ST.setCurrMethodId("main");
        String mainArgId = n.f11.accept(this, ST);
        ST.getCurrMethod().insertVar(mainArgId, "StringArray", "param");

        n.f14.accept(this, ST);

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

        String classId = n.f1.accept(this, ST);
        if (!ST.insertClass(classId, null))
        {
            throw new Exception("-> Error at line #" + n.f0.beginLine +
                    ": Class '" + classId + "' has been declared previously");
        }

        n.f3.accept(this, ST);
        n.f4.accept(this, ST);

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
        String classId = n.f1.accept(this, ST);
        String parentClassId= n.f3.accept(this, ST);
        
        if (parentClassId.equals(ST.getMainClassId()))
        {
	        throw new Exception("-> Error at line #" + n.f0.beginLine +
	                            ": Class '" + classId + "' extends Main Class '"
	                            + ST.getMainClassId() + "'");
        }
        
        //check if parent class has already been declared
        if (!ST.classExists(parentClassId))
        {
            throw new Exception("-> Error at line #" + n.f0.beginLine +
                    ": Class '" + classId + "' extends Class '" +
                    parentClassId + "' which has not been declared previously");
        }

        if (!ST.insertClass(classId, parentClassId))
        {
            throw new Exception("-> Error at line #" + n.f0.beginLine +
                    ": Class '" + classId + "' has been declared previously");
        }

        n.f5.accept(this, ST);
        n.f6.accept(this, ST);

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
        String methodId = n.f2.accept(this, ST);
        if (!ST.getCurrClass().insertMethod(methodId, methodRetType))
        {
            throw new Exception("-> Error at line #" + n.f0.beginLine +
                    ": A method '" + methodId + "' has been declared previously inside Class '" +
                    ST.getCurrClass().getID() + "'");
        }
        ST.setCurrMethodId(methodId);

        n.f4.accept(this, ST);
	    //check if particular method is inherently polymorphic
	    String report = ST.getCurrMethod().checkInheritance(Integer.toString(n.f3.beginLine));
	    if (!report.equals("OK"))
	    {
		    throw new Exception(report);
	    }
	
        n.f7.accept(this, ST);

        ST.setCurrMethodId(null);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable ST) throws Exception {
        String paramType = n.f0.accept(this, ST);
        String paramId  = n.f1.accept(this, ST);

        if (!ST.getCurrMethod().insertVar(paramId, paramType, "param"))
        {
            throw new Exception("-> Error at line #" + ST.getCurrLineNum() +
                    ": There is already a parameter '" + paramId + "' in the declaration of '" +
                    ST.getCurrMethodId() + "' method");
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
        String varId = n.f1.accept(this, ST);

        if (ST.getCurrMethod() != null)
        {
            if (!ST.getCurrMethod().insertVar(varId, varType, "var")) {
                throw new Exception("-> Error at line #" + ST.getCurrLineNum() +
                        ": A variable '" + varId + "' has been declared previously inside method '"
                        + ST.getCurrMethodId() + "'");
            }
        }
        else
        {
            if (!ST.getCurrClass().insertVar(varId, varType)) {
                throw new Exception("-> Error at line #" + ST.getCurrLineNum() +
                        ": A class field '" + varId + "' has been declared previously inside Class '"
                        + ST.getCurrClass().getID() + "'");
            }
        }

        return null;
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
        return n.f0.toString();
    }
    
}
