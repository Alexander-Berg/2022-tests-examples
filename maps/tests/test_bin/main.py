import os
import sys


def main():
    if len(sys.argv) == 1:
        sys.stdout.write("Hello, world!")
        sys.stderr.write("Hello, errors!")

    elif len(sys.argv) == 2:
        if sys.argv[1] == "check environment":
            if os.environ["var_name"] == "good value":
                sys.stdout.write("good environment")
            else:
                sys.stderr.write("bad environment `{}`".format(os.environ["var_name"]))
                exit(1)

        elif sys.argv[1] == "check stdin":
            data = sys.stdin.readline()
            sys.stdout.write(data)

        elif sys.argv[1] == "check argument":
            sys.stdout.write("good argument")

        else:
            sys.stderr.write("bad argument `{}`".format(sys.argv[1]))
            exit(1)
