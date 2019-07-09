package ClassInfo;
import java.util.*;
import MethodInfo.*;

public class ClassInfo {

    private String id;
    private ClassInfo parentClassInfo;

    private Map<String, String> vars;        //var name, var type
    private Map<String, MethodInfo> methods; //method name, methodInfo class ref
	
	//-----used in producing LLVM code-----
	private Map<String, ClassInfo> parentsClassesInfoReversed;
    private Map<String, MethodInfo> inheritedMethodsInfo; //keeps refs for all class's methods
    
	private Map<String, String> fieldsOffsets;  //fieldId, offset
	private Map<String, String> methodsOffsets; //methodId, offset
    private int VtableSize;
    private int fieldsSize;

    public ClassInfo(String classId, ClassInfo parentClassInfo)
    {
        this.id = classId;
        this.parentClassInfo = parentClassInfo;
        this.vars = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
	
	    //------used in producing LLVM code------
	    this.parentsClassesInfoReversed = new LinkedHashMap<>();
        this.inheritedMethodsInfo = new LinkedHashMap<>();
        this.fieldsOffsets = new LinkedHashMap<>();
	    this.methodsOffsets = new LinkedHashMap<>();
	    this.VtableSize = 0;
	    this.fieldsSize = 0;
    }

    public void print()
    {
        System.out.println("\t-> Class Name: " + id +
                (parentClassInfo != null ? " extends " + parentClassInfo.getID(): ""));

        for (Map.Entry<String, String> var : vars.entrySet())
        {
            System.out.println("\t\t- " + var.getValue() + " " + var.getKey());
        }

        for (Map.Entry<String, MethodInfo> methodInfo : methods.entrySet())
        {
            methodInfo.getValue().print();
        }
        System.out.println("\t============================");
    }
    
    public void printOffsets()
    {
	    //fieldsSize, VtableSize originally initialized to 0
	    if (this.parentClassInfo != null)
	    {
		    this.fieldsSize = this.parentClassInfo.getFieldsSize();
		    this.VtableSize = this.parentClassInfo.getVtableSize();
	    }
	    
	    System.out.println("\t---Variables---");
	    for (Map.Entry<String, String> var : vars.entrySet())
	    {
		    System.out.println("\t" + this.id + "." + var.getKey() + " : " + Integer.toString(this.fieldsSize));
		    this.fieldsSize += getBytes(var.getValue());
	    }
	
	    System.out.println("----------------------------");
	
	    System.out.println("\t---Methods---");
	    for (Map.Entry<String, MethodInfo> methodInfo : methods.entrySet())
	    {
		    if (!methodInfo.getValue().overridesParentMethod())
		    {
			    System.out.println("\t" + this.id + "." + methodInfo.getKey() + " : " + Integer.toString(this.VtableSize));
			    this.VtableSize += 8;
		    }
	    }
    }
    
    private int getBytes(String varType)
    {
        if (varType.equals("boolean"))
        {
        	return 1;
        }
        else if (varType.equals("int"))
        {
        	return 4;
        }
        else if (varType.equals("array"))
        {
        	return 8;
        }
        else
        {// Class type
	        return 8;
        }
    }
    
    public void storeOffsets()
    {
    	//fieldsSize, VtableSize originally initialized to 0
	    if (this.parentClassInfo != null)
	    {
		    this.fieldsSize = this.parentClassInfo.getFieldsSize();
		    this.VtableSize = this.parentClassInfo.getVtableSize();
	    }
	
	    for (Map.Entry<String, String> var : vars.entrySet())
	    {
		    this.fieldsOffsets.put(var.getKey(), Integer.toString(this.fieldsSize));
		    this.fieldsSize += getBytes(var.getValue());
	    }
	    
	    for (Map.Entry<String, MethodInfo> methodInfo : methods.entrySet())
	    {
		    if (!methodInfo.getValue().overridesParentMethod())
		    {
			    this.methodsOffsets.put(methodInfo.getKey(), Integer.toString(this.VtableSize));
			    this.VtableSize += 8;
		    }
	    }
    }
    
    public String getID()
    {
        return id;
    }

    public Map <String, String> getVars()
    {
        return vars;
    }
    
    public String getVarType(String varId)
    {
    	return vars.get(varId);
    }

    public Map <String, MethodInfo> getMethods()
    {
        return methods;
    }

    public MethodInfo getMethod(String methodId)
    {
        return methods.get(methodId);
    }
    
    public ClassInfo getParentClassInfo()
    {
    	return parentClassInfo;
    }

    public boolean insertVar(String varId, String varType)
    {
        if (!varExists(varId))
        {
            vars.put(varId, varType);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean varExists(String varId)
    {
        return vars.containsKey(varId);
    }

    public boolean insertMethod(String methodId, String methodRetType)
    {
        if (!methodExists(methodId))
        {
            MethodInfo methodInfo = new MethodInfo(methodId, methodRetType, this);
            methods.put(methodId, methodInfo);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean methodExists(String methodId)
    {
        return methods.containsKey(methodId);
    }
    
    public String getVarTypeFromParent(String varId)
    {// checking if variable has been declared in parent class(es) and has been inherited
	    ClassInfo tmpClassInfo = this.parentClassInfo;
	    while (tmpClassInfo != null)
	    {
	    	if (tmpClassInfo.varExists(varId))
		    {
		    	return tmpClassInfo.getVarType(varId);
		    }
	    	
		    tmpClassInfo = tmpClassInfo.getParentClassInfo();
	    }
	    
	    return null;
    }
	
	public MethodInfo getMethodInfoFromParent(String methodId)
	{// checking if method has been declared in parent class(es) and has been inherited
		ClassInfo tmpClassInfo = this.parentClassInfo;
		while (tmpClassInfo != null)
		{
			if (tmpClassInfo.methodExists(methodId))
			{
				return tmpClassInfo.getMethod(methodId);
			}
			
			tmpClassInfo = tmpClassInfo.getParentClassInfo();
		}
		
		return null;
    }
    
	//-----used in producing LLVM code-----
	
	public int getVtableSize()
	{
		return this.VtableSize;
	}
	
	public int getFieldsSize()
	{
		return this.fieldsSize;
	}
	
	public Map<String, String> getFieldsOffsets()
	{
		return this.fieldsOffsets;
	}
	
	public Map<String, String> getMethodsOffsets()
	{
		return this.methodsOffsets;
	}

    public void setParentsClassesInfoReversed()
    {
	    List<String> parentsClassesIds = new ArrayList<>();
	    List<ClassInfo> parentsClassesInfo = new ArrayList<>();
	    
	    ClassInfo tmpClassInfo = this.parentClassInfo;
		while (tmpClassInfo != null)
		{
			parentsClassesIds.add(tmpClassInfo.getID());
			parentsClassesInfo.add(tmpClassInfo);
			tmpClassInfo = tmpClassInfo.getParentClassInfo();
		}
	    
	    for(int i = parentsClassesIds.size() - 1; i >= 0; i--)
	    {
	    	this.parentsClassesInfoReversed.put(parentsClassesIds.get(i), parentsClassesInfo.get(i));
	    }
    }
    
    public void setInheritedMethodsInfo()
    {// store all class's methods (both native and inherited)
	    for (Map.Entry<String, ClassInfo> classInfo : this.parentsClassesInfoReversed.entrySet())
	    {
            for (Map.Entry<String, MethodInfo> methodInfo : classInfo.getValue().getMethods().entrySet())
            {
            	this.inheritedMethodsInfo.put(methodInfo.getKey(), methodInfo.getValue());
            }
        }
	
	    for (Map.Entry<String, MethodInfo> methodInfo : this.methods.entrySet())
	    {
		    this.inheritedMethodsInfo.put(methodInfo.getKey(), methodInfo.getValue());
	    }
    }

    public Map <String, MethodInfo> getInheritedMethodsInfo()
    {
        return this.inheritedMethodsInfo;
    }
    
    public int getMethodOffset(String methodId)
    {
	    for (Map.Entry<String, ClassInfo> classInfo : this.parentsClassesInfoReversed.entrySet())
	    {
	    	Map <String, String> classMethodsOffsets = classInfo.getValue().getMethodsOffsets();
	        if (classMethodsOffsets.containsKey(methodId))
	        {
	        	return Integer.parseInt(classMethodsOffsets.get(methodId));
	        }
	    }
	
	    return Integer.parseInt(this.methodsOffsets.get(methodId));
    }
	
    public int getFieldOffset(String varId)
    {
    	if (this.fieldsOffsets.containsKey(varId))
	    {
	    	return Integer.parseInt(this.fieldsOffsets.get(varId));
	    }
	
	    ClassInfo tmpClassInfo = this.parentClassInfo;
	    while (tmpClassInfo != null)
	    {
		    if (tmpClassInfo.getFieldsOffsets().containsKey(varId))
		    {
			    return Integer.parseInt(tmpClassInfo.getFieldsOffsets().get(varId));
		    }
		
		    tmpClassInfo = tmpClassInfo.getParentClassInfo();
	    }
	    
	    return -1;
    }
    
    public void clearLLVMmethodsVars()
    {
	    for (Map.Entry<String, MethodInfo> methodInfo : methods.entrySet())
	    {
		    methodInfo.getValue().clearLLVMvars();
	    }
    }
}
