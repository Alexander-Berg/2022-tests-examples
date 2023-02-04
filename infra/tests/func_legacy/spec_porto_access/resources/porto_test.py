#!/usr/bin/python

from time import sleep
import porto
import sys

def run_test(stdout_path):
    c = porto.Connection()
    r = c.Create("a")
    r.SetProperty("command", "/bin/echo -n OK")
    r.SetProperty("stdout_path", stdout_path)
    r.Start()
    r.Wait()

    with open(stdout_path) as f:
        stdout = f.read()

    stderr = r.GetProperty("stderr")

    r.Destroy()

    assert stdout == 'OK'
    assert len(stderr) == 0

    sleep(1000000)

if __name__ == '__main__':
    run_test(sys.argv[1])
