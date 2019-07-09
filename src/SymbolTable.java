import java.util.*;
import ClassInfo.*;
import MethodInfo.*;

public class SymbolTable {

    private Map<String, ClassInfo> classesInfo;  //class name, ClassInfo class ref
    private ClassInfo currClass; //used for keeping class scope
    private String currMethodId; //used for keeping method scope
    private int currLineNum;
    private String mainClassId;
	
    //used for keeping track of nested method calls
    private Stack<MethodInfo> methodsCallStack;
	private Stack<Integer> methodsCallArgIndex;
	
	
    public SymbolTable()
    {
        classesInfo = new LinkedHashMap<>();
        methodsCallStack = new Stack<>();
        methodsCallArgIndex = new Stack<>();
        currClass = null;
        currMethodId = null;
        currLineNum = 0;
    }

    public void print()
    {
        System.out.println("--> Printing Symbol Table <--");
        for (Map.Entry<String, ClassInfo> classInfo : classesInfo.entrySet())
        {
            classInfo.getValue().print();
        }
    }
    
    public void printOffsets()
    {
	    System.out.println("\n==> Printing Offsets <==\n");
	    for (Map.Entry<String, ClassInfo> classInfo : classesInfo.entrySet())
	    {
	    	if (classInfo.getKey() == this.mainClassId)
		    {
		    	continue;
		    }
		
		    System.out.println("-----------Class " + classInfo.getKey() + "-----------");
		    classInfo.getValue().printOffsets();
//		    System.out.println("\t----------------------------");
		    System.out.println("============================\n");
//		    System.out.println("\n");
	    }
    }
	
	public void storeOffsets()
	{
		for (Map.Entry<String, ClassInfo> classInfo : classesInfo.entrySet())
		{
			if (classInfo.getKey() == this.mainClassId)
			{
				continue;
			}
			
			classInfo.getValue().storeOffsets();
		}
	}

    public int getCurrLineNum()
    {
        return currLineNum;
    }

    public void setCurrLineNum(int lineNum)
    {
        this.currLineNum = lineNum;
    }
    
    public String getMainClassId()
    {
    	return mainClassId;
    }
    
    public void setMainClassId(String mainClassId)
    {
    	this.mainClassId = mainClassId;
    }

    public ClassInfo getCurrClass()
    {
        return currClass;
    }
    
    public void setCurrClassById(String classId)
    {
    	this.currClass = getClassInfo(classId);
    }

    public ClassInfo getClassInfo(String classId)
    {
        return classesInfo.get(classId);
    }

    public MethodInfo getCurrMethod()
    {
        return currClass.getMethod(currMethodId);
    }

    public String getCurrMethodId()
    {
        return currMethodId;
    }

    public void setCurrMethodId(String currMethodId)
    {
        this.currMethodId = currMethodId;
    }

    public Map <String, ClassInfo> getClassesInfo ()
    {
        return classesInfo;
    }

    public boolean insertClass(String classId, String parentClassId)
    {
        if (!classExists(classId))
        {
            ClassInfo classInfo = new ClassInfo(classId, getClassInfo(parentClassId));
            classesInfo.put(classId, classInfo);

            currClass = classInfo;
            currMethodId = null;

            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean classExists(String classId)
    {
        return classesInfo.containsKey(classId);
    }

    public void resetCurrState()
    {
	    currClass = null;
	    currMethodId = null;
	    currLineNum = 0;
    }
    
    public boolean isPrimitiveType(String varType)
    {
    	return varType.equals("int") || varType.equals("boolean") || varType.equals("array");
    }
	
	public boolean isParentOf(String parentClassId, String childClassId)
	{
		ClassInfo tmpClassInfo = getClassInfo(childClassId);
		while (tmpClassInfo != null)
		{
			if (tmpClassInfo.getID().equals(parentClassId))
			{
				return true;
			}
			
			tmpClassInfo = tmpClassInfo.getParentClassInfo();
		}
		
		return false;
	}
	
	public void pushMethodInfoCall(MethodInfo methodInfo)
	{
		methodsCallStack.push(methodInfo);
	}
	
	public void popMethodInfoCall()
	{
		methodsCallStack.pop();
	}
	
	public MethodInfo getMethodInfoCall()
	{
		return methodsCallStack.peek();
	}
	
	public void pushMethodArgIndex()
	{
		methodsCallArgIndex.push(0);
	}
	
	public void popMethodArgIndex()
	{
		methodsCallArgIndex.pop();
	}
	
	public Integer getMethodArgIndex()
	{
		return methodsCallArgIndex.peek();
	}
	
	public void increaseMethodArgIndex()
	{
		methodsCallArgIndex.push(methodsCallArgIndex.pop() + 1);
	}
	
	public String getVarType(String varId)
	{// checking in current's or parent's class scope for getting variable's type
		if (getCurrMethod().varExists(varId, "var"))
		{
			return getCurrMethod().getVarType(varId);
		}
		else if (getCurrClass().varExists(varId))
		{
			return getCurrClass().getVarType(varId);
		}
		else
		{
			return getCurrClass().getVarTypeFromParent(varId);
		}
	}
	
	public Integer checkAssignmentLegitimacy(String leftType, String rightType)
	{
		if (!leftType.equals(rightType))
		{
			if (isPrimitiveType(leftType) || isPrimitiveType(rightType))
			{
				return 0;
			}
			else if (!isParentOf(leftType, rightType))
			{
				return -1;
			}
		}
		
		return 1;
	}
	
	public boolean isID(String id)
	{
		return id.startsWith("@");
	}
	
	public String extractId(String id)
	{
		return id.replace("@","");
	}
	
	public boolean isInteger(String input)
	{
		try
		{
			Integer.parseInt(input);
			return true;
		}
		catch(NumberFormatException e)
		{
			return false;
		}
	}
	
	public void clearLLVMmethodsVars()
	{
		for (Map.Entry<String, ClassInfo> classInfo : this.classesInfo.entrySet())
		{
			classInfo.getValue().clearLLVMmethodsVars();
		}
	}
	
}