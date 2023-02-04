arcadia_root := $(shell ya dump root)
params := $(wordlist 2,100,$(MAKECMDGOALS))
pytest_args := -m pytest -p no:warnings -sv --alluredir ./.allure
source_root := Y_PYTHON_SOURCE_ROOT=$(arcadia_root)
entry_point := Y_PYTHON_ENTRY_POINT=":main"

# Code style
check-flake8:
	flake8 ./ --ignore=E501,W503

# Development
build:
	ya make integration

dev:
	 $(source_root) $(entry_point) ./integration/billing-hot-tests-integration $(pytest_args) $(if $(params),$(params),integration)

# Tests
large-tests:
	ya make -A --test-tag=ya:manual -k $(params)

medium-tests:
	ya make -A --test-size medium -k $(params)

allure:
	allure serve .allure



