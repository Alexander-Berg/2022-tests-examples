from setuptools import setup

setup(
    name='pytest-teamcity',
    py_modules=['pytest_teamcity'],
    entry_points={'pytest11': ['teamcityex = pytest_teamcity']},
    install_requires=['teamcity-messages']
)
