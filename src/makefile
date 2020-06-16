all: compile

compile:
	java -jar ../Jars/jtb132di.jar -te minijava.jj
	java -jar ../Jars/javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -f *.class *~ minijava-jtb.jj ./MethodInfo/*.class ./ClassInfo/*.class MiniJavaParser.java MiniJavaParserConstants.java JavaCharStream.java ParseException.java MiniJavaParserTokenManager.java Token.java TokenMgrError.java && rm -r syntaxtree visitor

delete_tests:
	rm -f ./Tests/*

add_inputs:
	cp ../Inputs/* ./Tests
	
test_outputs:
	bash ./testOutputs.sh