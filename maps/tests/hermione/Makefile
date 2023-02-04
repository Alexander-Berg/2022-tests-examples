OPT_NODEJS := /opt/nodejs/8/bin

NODE := $(shell command -v $(OPT_NODEJS)/node 2>/dev/null)
ifndef NODE
	NODE := node
endif
NPM := $(shell command -v $(OPT_NODEJS)/npm 2>/dev/null)
ifndef NPM
	NPM := npm
endif

NODE_MODULES_BIN := node_modules/.bin
HERMIONE := $(NODE_MODULES_BIN)/hermione

GRID_URL := http://selenium:selenium@sg.yandex-team.ru:4444/wd/hub

deps:
	@PATH=$(OPT_NODEJS):$(PATH) $(NPM) --registry http://npm.yandex-team.ru/ install

run:
	cp ./.testing.conf.js ./.hermione.conf.js
	@hermione_grid_url=$(GRID_URL) $(NODE) $(HERMIONE) --set func

run-smoke:
	cp ./.common.conf.js ./.hermione.conf.js
	@hermione_grid_url=$(GRID_URL) $(NODE) $(HERMIONE) --set func --browser chrome --grep smoke --browsers-chrome-sessions-per-browser 20

run-update:
	npx hermione gui

run-local:
	cp ./.testing-local.conf.js ./.hermione.conf.js
	@hermione_grid_url=$(GRID_URL) $(NODE) $(HERMIONE) --set func
deploy-test-servers:
	sh ./utils/local-run/deploy-test-pages.sh
