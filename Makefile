all:
	$(info "Try: 'make compile' or 'make watch'")

compile:
	npx shadow-cljs compile dev

watch:
	npx shadow-cljs watch dev

.PHONY: all compile watch
