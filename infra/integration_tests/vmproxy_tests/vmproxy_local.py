import argparse
from infra.swatlib import cmdutil
from infra.qyp.vmproxy.src import application


def init_arg_parser():
    parser = argparse.ArgumentParser(prog=application.Application.name)
    parser.add_argument('-c', '--cfg',
                        default=None,
                        action='store', help='path to service cfg file')
    parser.add_argument('--console',
                        default=False,
                        action='store_true',
                        help='redirect log to stdout (overrides config)')
    return parser


def run(argv):
    arg_parser = init_arg_parser()
    cmdutil.main(arg_parser, application.Application, env_prefix=None, argv=argv)
