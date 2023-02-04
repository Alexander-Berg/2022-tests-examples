# coding: utf-8
import argparse

import pip
import pip.req
import pkg_resources

from btestlib import utils


def parse_arguments():
    parser = argparse.ArgumentParser()
    # todo-igogor сделать опциональными?
    parser.add_argument('adapter')
    parser.add_argument('report')
    adapter_version, report_version = parser.parse_args()
    return adapter_version, report_version


def parse_requirements():
    requirements = pip.req.parse_requirements(utils.project_file('requirements.txt'), session="somesession")
    list_requirements = list(requirements)
    for item in requirements:
        if isinstance(item, pip.req.InstallRequirement):
            print("required package: {}".format(item.name))

            if len(str(item.req.specifier)) > 0:
                print("  " + str(item.req.specifier))

            if item.link is not None:
                print("  from: " + item.link.url)
                print("  filename: " + item.link.filename)
                print("  egg: " + item.link.egg_fragment)

            if len(item.options) > 0:
                for opt_type, opts in item.options.iteritems():
                    print("  {}:".format(opt_type))
                    if type(opts) is list:
                        for opt in opts:
                            print("    " + opt)
                    elif type(opts) is dict:
                        for k, v in opts.iteritems():
                            print("    {}: {}".format(k, v))


def uninstall_allure_pytest():
    # todo-igogor что будет если пакета не было установлено?
    pip.main(['uninstall', 'allure-pytest'])
    pip.main(['uninstall', 'allure-python-commons'])


def install_allure_pytest():
    pass


if __name__ == '__main__':
    parse_requirements()
    allure_pytest_version = pkg_resources.get_distribution("allure-pytest").version
    allure_python_commons_version = pkg_resources.get_distribution("allure-python-commons").version

    pass
