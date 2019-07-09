package MethodInfo;
import java.util.*;
import ClassInfo.*;

public class MethodInfo {

    private String returnType;
    private String id;
    private ClassInfo memberOf;
    private boolean overridesParentMethod;

    private Map<String, String> vars;    //varId, varType
    private Map<String, String> params;  //paramId, paramType
    
    //-----used in producing LLVM code-----
	private Map<String, String> LLVMvars; //varId, LLVMvarId

    public MethodInfo(String methodId, String methodReturnType, ClassInfo hostClassInfo)
    {
    	overridesParentMethod = false;
        returnType = methodReturnType;
        id = methodId;
        memberOf = hostClassInfo;
        vars = new LinkedHashMap<>();
        params = new LinkedHashMap<>();
	
	    //-----used in producing LLVM code-----
	    LLVMvars = new LinkedHashMap<>();
    }

    public void print()
    {
        System.out.println("\t\t----------------------------");

        System.out.print("\t\t> " + returnType + " " + id + "(");
        for (Map.Entry<String, String> param : params.entrySet())
        {
            System.out.print(param.getValue() + " " + param.getKey() + ", ");
        }
        System.out.println(")");

        for (Map.Entry<String, String> var : vars.entrySet())
        {
            System.out.print("\t\t\t~ ");
            System.out.println(var.getValue() + " " + var.getKey());
        }
    }
    
    public String getID()
    {
    	return id;
    }
    
    public String getReturnType()
    {
    	return returnType;
    }
    
    public ClassInfo getHostClassInfo()
    {
    	return this.memberOf;
    }
    
    public Map<String, String> getParameters()
    {
    	return params;
    }
    
    public boolean overridesParentMethod()
    {
    	return this.overridesParentMethod;
    }
    
    public Integer getParametersNumber()
    {
    	return params.size();
    }
    
    public String getVarType(String varId)
    {
	    if (vars.get(varId) != null)
	    {
	    	return vars.get(varId);
        }
	    else
	    {
		    return params.get(varId);
	    }
    }
    
    public String getParameterType(Integer paramIndex)
    {//Based on index 
	    if (params.size() <= paramIndex)
	    {
	    	return null;
	    }
	    
	    List<String> paramsTypes = new ArrayList<>(params.values());
    	return paramsTypes.get(paramIndex);
    }

    public boolean insertVar (String varId, String varType, String var_or_param)
    {
        if (!varExists(varId, var_or_param))
        {
            if (var_or_param.equals("var"))
            {
                vars.put(varId, varType);
            }
            else
            {
                params.put(varId, varType);
            }
            return true;
        }
        else
        {
            return false;
        }

    }

    public boolean varExists (String varId, String var_or_param)
    {
        if (var_or_param.equals("var"))
        {
            return vars.containsKey(varId) || params.containsKey(varId);
        }
        else
        {
            return params.containsKey(varId);
        }
    }
    
	public String checkInheritance(String lineNum)
	{
		ClassInfo tmpClassInfo = this.memberOf.getParentClassInfo();
		while (tmpClassInfo != null)
		{
			MethodInfo tmpMethodInfo = tmpClassInfo.getMethod(id);
			if (tmpMethodInfo != null)
			{
				if (tmpMethodInfo.getReturnType() != this.returnType)
				{
					String err = "-> Error at line #" + lineNum + ": Method '" + this.id + "' in Class '" +
							     this.memberOf.getID() + "' overrides method in Class '" + tmpClassInfo.getID()
							             + "' but have different return types";
					return err;
				}
				
				Map<String, String> tmpMethodParams = tmpMethodInfo.getParameters();
				if (tmpMethodParams.size() != this.params.size())
				{
					String err = "-> Error at line #" + lineNum + ": Method '" + this.id + "' in Class '" +
							             this.memberOf.getID() + "' overrides method in Class '" + tmpClassInfo.getID()
							             + "' but have different number of parameters";
					return err;
				}
				
				Iterator<String> thisParamsTypes = this.params.values().iterator();
				Iterator<String> tmpMethodParamsTypes = tmpMethodParams.values().iterator();
				int i=0;
				while (thisParamsTypes.hasNext())
				{
					i++;
					if (thisParamsTypes.next() != tmpMethodParamsTypes.next())
					{
						String err = "-> Error at line #" + lineNum + ": Method '" + this.id + "' in Class '" +
								             this.memberOf.getID() + "' overrides method in Class '" + tmpClassInfo.getID()
								             + "' but parameter #" + Integer.toString(i) + " is of different type";
						return err;
					}
				}
				
				this.overridesParentMethod = true;
			}
			
			tmpClassInfo = tmpClassInfo.getParentClassInfo();
		}
		
		return "OK";
	}
	
	//-----used in producing LLVM code-----
	
	public void insertLLVMvar(String varId, String LLVMvarId)
	{
		this.LLVMvars.put(varId, LLVMvarId);
	}
	
	public String getLLVMvar(String varId)
	{
		return this.LLVMvars.get(varId);
	}
	
	public void clearLLVMvars()
	{
		this.LLVMvars.clear();
	}
	
	public boolean LLVMvarExists(String varId)
	{
		return this.LLVMvars.containsKey(varId);
	}
	
}