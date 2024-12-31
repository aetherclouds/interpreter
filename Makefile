# JCFLAGS := -g

.PHONY: run runjar clean
.SECONDARY: # all targets are secondary (intermediate files are not removed). tis is necessary
# because `run-%` is not a valid .PHONY target, so its dependencies are treated as secondary

run: bin/lox/Lox.class
#	`--classpath` behaves as a root
	java -cp bin lox.Lox
#	we can't do `java bin.lox.Lox` because the `package` structure wouldn't match
#	make starts a shell for each line, which means we have to chain `cd`.
#	cd bin && java lox.Lox

run-% : bin/lox/%.class
	java -cp bin lox.$(subst /,.,$*)

runjar: bin/lox.jar
	java -jar bin/lox.jar

bin/lox.jar: bin/lox/Lox.class
#	the file(/classpath?) structure within a .jar, as well as the main classpath (`lox.Lox`),
#	has to matck the source code's `package` structure
#	so we can't do `[...] Lox -C bin/lox .` for example.
#	also, apparently, `-C` is a standard parameter to `cd` internally. ex. `make -C somedir`
	jar -cvfe $@ lox.Lox -C bin/ lox/ # no need to pass specific files, it expands dirs (i.e. `lox/`)

# NOTE: `**` is not supproted in some GNU Make versions
# recompile when a change is detected in any file
bin/lox/%.class: $(wildcard src/lox/*.java) $(wildcard src/lox/**/*.java)
# 	javac automatically compiles dependencies, so even if we only asked for
#	Lox.class, it'll also output Scanner.class, Token.class, etc.
	javac $(JCFLAGS) src/lox/$*.java -d bin -sourcepath src
#					          ^^^^^^^^^^^^^^^
# this is so that we can write `package lox;` instead of `package src.lox;`

# clean:
# 	\rm -rf bin/*

# an interesting thing about `%.class: %.java` is that Make
# will not match this target if there if there is no rule or file
# matching its dependency. so if we missed the letter casing, e.g.
# `make bin/lox.class` vs `make bin/Lox.class`, the former
# would be a make error.
