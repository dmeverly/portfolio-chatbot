SHELL := /bin/bash

.PHONY: run clean dev

run:
	@echo "Building and starting production server..."
	npm run build
	exec npm start

dev:
	@echo "Starting dev server..."
	exec npm run dev

clean:
	@echo "Cleaning logs..."
	- rm -rf logs || true
	@echo "clean complete"
