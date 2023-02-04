# local mirror
: ${python_pip_url="https://pypi.yandex-team.ru/simple"}

: ${with_python2="1"}
: ${with_python3="1"}

tee /etc/pip.conf <<EOF
[global]
index-url = ${python_pip_url}
timeout = 120
EOF

export DEBIAN_FRONTEND="noninteractive"

if [ "$with_python2" = "1" ] ; then
	apt-get --quiet --yes --no-install-recommends install \
		python \
		python-dev \
		python-pip \
		cython
	pip install --upgrade --index-url=${python_pip_url} "pip < 21.0"

	python -V
fi

if [ "$with_python3" = "1" ] ; then
	apt-get --quiet --yes --no-install-recommends install \
		python3 \
		python3-dev \
		python3-pip \
		cython3
	# setuptools 50.0 is broken https://github.com/pypa/setuptools/issues/2352
	# setuptools 51.0+ dropped support for python 3.5: https://setuptools.pypa.io/en/latest/history.html#v51-0-0
	pip3 install --upgrade --index-url=${python_pip_url} "setuptools!=50.0,<51.0"
	pip3 install --upgrade --index-url=${python_pip_url} "pip < 21.0"

	python3 -V
fi
