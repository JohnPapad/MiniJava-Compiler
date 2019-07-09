all: compile

compile:
	java -jar ../Jars/jtb132di.jar -te minijava.jj
	java -jar ../Jars/javacc5.jar minijava-jtb.jj
	javac Main.java


clean:
	rm -f *.class *~
