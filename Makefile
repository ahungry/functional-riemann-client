all:
	$(info "Try: 'make compile' or 'make watch'")

test:
	node index.js

repl:
	npx shadow-cljs node-repl dev

compile: lib/src/main/fnrc/proto.proto
	npx shadow-cljs compile dev

watch: lib/src/main/fnrc/proto.proto
	npx shadow-cljs watch dev

lib/src/main/fnrc/proto.proto:
	-mkdir -p ./lib/src/main/fnrc; cp ./src/main/fnrc/proto.proto ./lib/src/main/fnrc/

.PHONY: all compile watch
