import click
import os
import re
import shlex
import subprocess
import sys
import typing as tp


def read_testplan(plan_file_path: str) -> tp.Iterable[tuple[int, tp.Optional[str], tp.Optional[str], tp.Optional[str]]]:
    with open(plan_file_path, "r") as plan:
        line_number = 0

        def nextline():
            nonlocal line_number
            line_number += 1
            return plan.readline()

        while line := nextline():
            line = line.strip()
            if not line or line.startswith("#"):  # skip blanks and comments
                continue

            if line.startswith("<<"):  # tagged section
                separator = line[2:].strip()
                request = []
                while True:
                    line = nextline()
                    if not line:
                        raise StopIteration('Unexpected end of file')
                    if line.startswith(separator):  # tagged section finished
                        break
                    request.append(line)

                line = line[len(separator):]
                empty, field, command = (line.split('|', 2) + [None, None])[:3]
                if empty.strip():
                    print(f'\n{plan_file_path}:{line_number}: junk after request: {empty}', file=sys.stderr)
                request = ''.join(request)[:-1]
                request = request.replace('\\', '\\\\').replace('\n', '\\n')
                yield line_number, request, field, command
            else:
                request, field, command = (line.split('|', 2) + [None, None])[:3]
                yield line_number, request.strip(), field, command


@click.command()
@click.option('-a', '--app',  help="Application binary path", required=True)
@click.option('-p', '--plan',  help="Test plan file path, contains list of test queries and commands", required=True)
@click.option('-v', '--verbose', is_flag=True)
def main(app: str, plan: str, verbose: bool):
    APP = subprocess.Popen(
        app,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=(None if verbose else subprocess.DEVNULL),
        shell=True
    )
    exit_status = 0
    tests = 0
    fails = 0

    try:
        last_request: tp.Optional[str] = None
        for lineno, request, field, command in read_testplan(plan):
            if request:
                if not verbose:
                    sys.stderr.write('.')  # progress indicator
                last_request = request
                APP.stdin.write(f'{request}\n'.encode())
                APP.stdin.flush()

                headers = {}
                line = APP.stdout.readline().strip().decode()
                while not line and APP.poll() is None:
                    line = APP.stdout.readline().strip().decode()
                while line:
                    k, v = line.split(': ', 1)
                    headers[k] = v
                    line = APP.stdout.readline().decode().strip()

                if not request.startswith("HEAD "):
                    body = APP.stdout.read(int(headers['Content-Length'])).decode()
                else:
                    body = ""

            if not field or not command:
                continue
            if not verbose:
                sys.stderr.write('.')  # progress indicator
            field = field.strip()
            command = command.strip()

            tests += 1
            if field != "headers" and field != "body" and field != "all":
                print(f'\n{plan}:{lineno}: unknown field {field}', file=sys.stderr)
                fails += 1
                continue

            check_data = ""
            if field == "headers" or field == "all":
                for k, v in headers.items():
                    check_data += k + ": " + v + '\n'
            if field == "all":
                check_data += '\n'
            if field == "body" or field == "all":
                check_data += body

            failed = False
            command_parts = shlex.split(command)

            not_flag = False
            if command_parts[0] == '!':
                not_flag = True
                command_parts = command_parts[1:]

            if command_parts[0] == 'grep' and len(command_parts) == 2:
                failed = (re.search(command_parts[1], check_data) is None)
            elif command_parts[0] == 'diff' and len(command_parts) == 2:
                failed = (check_data != open(command_parts[1]).read())
            elif command_parts[0] == 'count_bytes' and len(command_parts) == 2:
                failed = (len(check_data) != int(command_parts[1]))
            elif command_parts[0] == 'cat' and len(command_parts) == 2:
                with open(command_parts[1], 'w') as f:
                    f.write(check_data)
            elif command_parts[0] == 'rm' and len(command_parts) == 2:
                os.remove(command_parts[1])
            else:
                raise RuntimeError(f'unknown command: {command}')

            if not_flag:
                failed = not failed

            if failed:
                fails += 1
                print(f'\n{plan}:{lineno}: test failed: {last_request} | {field} | {command}', file=sys.stderr)
                print('\n'.join((f'    {k}: {v}' for k, v in headers.items())), file=sys.stderr)
                print('\n    {}\n'.format(body.replace('\n', '\n    ')), file=sys.stderr)
    finally:
        APP.stdin.close()
        exit_status = APP.wait()

    if exit_status:
        print(f'\n{app} exited with code {exit_status}', file=sys.stderr)

    print(f'\n{tests} test(s) executed, {fails} test(s) failed', file=sys.stderr)
    sys.exit((fails or exit_status) and 1)


if __name__ == '__main__':
    main()
