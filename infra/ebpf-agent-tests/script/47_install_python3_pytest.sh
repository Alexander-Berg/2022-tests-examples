# Install python3 pytest tools
: ${python_pip_url="https://pypi.yandex-team.ru/simple"}

python3 -m pip install --upgrade --index-url=${python_pip_url} \
	pytest \
	pytest-xdist \
	pytest-metadata

