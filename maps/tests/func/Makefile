API_VERSION := 2.1.79
API_VERSION_REFERENCE := $(API_VERSION)
VECTOR_BUNDLE ?= 5.18.3

API_LANG := 'ru_RU'
API_HOSTS := 'prod'

SET := smoke

NPM_BIN = ./node_modules/.bin

NODE ?= node
NPM ?= npm

HERMIONE := $(NPM_BIN)/hermione

GRID_URL := http://geo@sw.yandex-team.ru:80/v0
LAUNCH := API_LANG=$(API_LANG) API_HOSTS=$(API_HOSTS) API_VERSION=$(API_VERSION) hermione_grid_url=$(GRID_URL) \
	$(HERMIONE) $(ARGS) --set $(SET)
PROD := ENVIRONMENT_REFERENCE=PRODUCTION ENVIRONMENT_ACTUAL=PRODUCTION
PROD_TILES := ENVIRONMENT_REFERENCE=TESTING_OVERRIDDEN_HOSTS ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS
CUSTOM ?= ./test-customization/configs/default.json
NIGHT ?= 0

install:
	PATH=$(NODE_BIN):$(PATH) $(NPM) --registry http://npm.yandex-team.ru/ install

lint:
	PATH=$(NODE_BIN):$(PATH) $(NPM_BIN)/eslint .

lint-fix:
	PATH=$(NODE_BIN):$(PATH) $(NPM_BIN)/eslint . --fix

test-reference:
	(TYPE=REFERENCE VECTOR=0 RETRY=2 $(LAUNCH) --update-refs && echo "success!") || echo "failure!"

test-actual:
	TYPE=ACTUAL VECTOR=1 RETRY=0 $(LAUNCH)

test-local:
	VECTOR=1 $(NODE) $(HERMIONE) --browser chrome --set func

test: test-reference test-actual

test-prod:
	$(PROD) make test

test-prod-tiles:
	$(PROD_TILES) make test

test-vector-prod-testing:
	VECTOR_ALL=1 ENVIRONMENT_REFERENCE=PRODUCTION ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS API_HOSTS='prod' make test

test-vector-datatesting:
	VECTOR_ALL=1 ENVIRONMENT_REFERENCE=PRODUCTION ENVIRONMENT_ACTUAL=TESTING API_HOSTS='datatesting' make test

test-map-design:
	VECTOR_ALL=1 ENVIRONMENT_REFERENCE=TESTING_WITH_PROD_TILES ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS make test SET=design API_HOSTS=design

test-map-design-regress:
	VECTOR_ALL=1 ENVIRONMENT_REFERENCE=TESTING_WITH_PROD_TILES ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS make test SET=designRegress API_HOSTS=design

test-vector-prod-testing-gui:
	VECTOR_ALL=1 ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS ARGS="gui $(ARGS)" make test-actual

test-get-start-time:
	rm -f ./reports/all-tests-time-1.txt ./reports/all-tests-time-2.txt ./reports/run-time-1.txt ./reports/run-time-2.txt
	for number in 1 2 3 4 5 6 7 8 9 10; do \
		make -i test-actual  GET_TILES_LOADED_TIME=1 ENVIRONMENT_ACTUAL=TESTING_OVERRIDDEN_HOSTS SET="performanceStartTime" $(ARGS) &&\
        	$(NODE) ./tools/write-run-time.js ; \
	done
	$(NODE) ./tools/get-average-time.js ;

test-customization-open-page:
	CUSTOM="$(CUSTOM)" VECTOR_BUNDLE="$(VECTOR_BUNDLE)" bash ./test-customization/open2page.sh

test-customization:
	rm -rf ./reports/hermione
	FLEX=1 RETRY=6 API_VERSION=$(API_VERSION) API_HOSTS='testing' VECTOR_BUNDLE="$(VECTOR_BUNDLE)" hermione_grid_url=$(GRID_URL) $(NODE) $(HERMIONE) --set customization

test-customization-gui:
	FLEX=1 RETRY=6 API_VERSION=$(API_VERSION) API_HOSTS='testing' VECTOR_BUNDLE="$(VECTOR_BUNDLE)" hermione_grid_url=$(GRID_URL) $(HERMIONE) gui --set customization

.PHONY: install test test-reference test-actual test-customization test-customization-open-page
