import syntaxtree.*;
import visitor.*;
import java.io.*;


public class Main {
    public static void main (String [] args)
    {
        if (args.length < 1)
        {
            System.err.println("-> Run program as: java Main <file1> <file2> <file3> ... <fileN> ");
            System.exit(1);
        }
        
        FileInputStream fis = null;
        BufferedWriter bwriter = null;

        for (int i = 0; i < args.length; i++)
        {
            try
            {
	            System.out.println("\n\n-> Producing LLVM code for input file '" + args[i] + "'");
    
                String rootDir = "Tests/";
                // Put all input files in "Tests" folder
                String inputFilePath = rootDir + args[i];
	            fis = new FileInputStream(inputFilePath);

                // Parse file
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();

                // Create Symbol Table object
	            SymbolTable ST = new SymbolTable();
	
	            // Fill Symbol Table using FillSTVisitor
                FillSTVisitor fillSTVisitor = new FillSTVisitor();
                root.accept(fillSTVisitor, ST);
                ST.resetCurrState();
                
	            // Perform type check using TypeCheckingVisitor
	            TypeCheckingVisitor typeCheckingVisitor = new TypeCheckingVisitor();
	            root.accept(typeCheckingVisitor, ST);

                ST.storeOffsets();
                ST.resetCurrState();
    
                String outputDir = "Tests/" + args[i]; //outputs will be placed to the same folder with corresponding inputs
		        outputDir = outputDir.substring(0, outputDir.length() - 4); //remove .java suffix
		        outputDir = outputDir + "ll"; //add LLVM's extension
                bwriter = new BufferedWriter(new FileWriter(outputDir));
	
	            ProduceLLVMVisitor produceLLVMVisitor = new ProduceLLVMVisitor(bwriter);
                root.accept(produceLLVMVisitor, ST);
                
            }
            catch (ParseException ex)
            {
                System.out.println(ex.getMessage());
            }
            catch (FileNotFoundException ex)
            {
                System.err.println(ex.getMessage());
            }
            catch (Exception ex)
            {
                System.out.println(ex.getMessage());
            }
            finally
            {
	            System.out.println("////////////////////////////////");
	            try
                {
                    if (fis != null)
                    {
                    	fis.close();
                    }

                    if (bwriter != null) 
                    {
                        bwriter.close();
                    }
                }
                catch (IOException ex)
                {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
