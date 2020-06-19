all: compile

compile:
	java -jar ../Jars/jtb132di.jar -te minijava.jj
	java -jar ../Jars/javacc5.jar minijava-jtb.jj
	javac Main.java

exec: 
	java Main test01.java test02.java test03.java test04.java test05.java test06.java test07.java test08.java test09.java test10.java test11.java test12.java test13.java test14.java test15.java test16.java test17.java test18.java test19.java test20.java test21.java test22.java test23.java

clean:
	rm -f *.class *~ minijava-jtb.jj ./MethodInfo/*.class ./ClassInfo/*.class MiniJavaParser.java MiniJavaParserConstants.java JavaCharStream.java ParseException.java MiniJavaParserTokenManager.java Token.java TokenMgrError.java && rm -r syntaxtree visitor

clean_tests:
	rm -f ./Tests/*

add_inputs:
	cp ../Inputs/* ./Tests
	
test_outputs:
	bash ./testOutputs.sh