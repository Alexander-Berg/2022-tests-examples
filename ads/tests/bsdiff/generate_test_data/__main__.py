import os
import random
import subprocess
import sys
import tarfile


def add_file(tgz, filename, filepath):
    with open(filepath, "rb") as f:
        info = tgz.gettarinfo(filepath)
        info.name = filename
        tgz.addfile(info, f)


def generate_file(filename):
    size = random.randint(100, 20000)
    with open(filename, "wb+") as output:
        output.write(os.urandom(size))


def generate_test_data(output_tgz, count, courgette_bsdiff):
    with tarfile.open(output_tgz, "w:gz") as tgz:
        for i in range(1, count):
            name = "test_{:06d}".format(i)
            base = name + ".base"
            generate_file(base)
            decoded = name + ".decode"
            generate_file(decoded)
            patch = name + ".patch"
            proc = subprocess.Popen([courgette_bsdiff, "--genbsdiff", base, decoded, patch])
            rc = proc.wait()
            add_file(tgz, base, base)
            add_file(tgz, patch, patch)
            os.remove(base)
            os.remove(decoded)
            os.remove(patch)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: generate_test_data <test_samples_count> <courgette_bsdiff_binary_path>")
        exit(-1)
    output_tgz = "bsdiff_test_data.tgz"
    tests_count = int(sys.argv[1])
    courgette_bsdiff = sys.argv[2]
    generate_test_data(output_tgz, tests_count, courgette_bsdiff)
