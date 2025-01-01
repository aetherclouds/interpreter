# JCFLAGS := -g

.PHONY: run runjar clean
.SECONDARY: # all targets are secondary (intermediate files are not removed)

# we have a `bin/lox/*` structure (instead of just `bin/*` to match the classpath structure `lox.*`
run: bin/lox/Lox.class
	java -cp bin lox.Lox

run-% : bin/lox/%.class
	java -cp bin lox.$(subst /,.,$*) $(RUNARGS)

runjar: bin/lox.jar
	java -jar bin/lox.jar $(RUNARGS)

bin/lox.jar: bin/lox/Lox.class
	jar -cvfe $@ lox.Lox -C bin/ lox/ # no need to pass specific files, it expands dirs (i.e. `lox/`)

# NOTE: `**` is not supproted in some GNU Make versions
# recompile when a change is detected in any file
bin/lox/%.class: $(wildcard src/lox/*.java) $(wildcard src/lox/**/*.java)
	javac $(JCFLAGS) src/lox/$*.java -d bin -sourcepath src

clean:
	\rm -rf bin/*