all:
	rm -f */*.class
	javac -cp .:lib/* Main.java
	java -cp .:lib/* Main

test:
	rm -f */*.class
	javac -cp .:lib/* Test.java
	java -cp .:lib/* Test