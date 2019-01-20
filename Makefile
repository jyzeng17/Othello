CLASSPATH = -cp target/othello-1.0-SNAPSHOT-jar-with-dependencies.jar
MAINCLASS = io.github.jyzeng17.othello.Othello

all:
	mvn clean package

run:
	java $(CLASSPATH) ${MAINCLASS}

clean:
	rm logs/*
