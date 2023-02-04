import argparse
import subprocess


REGIONS_1 = [
    "POLYGON ((1 0, 1 1, 2 1, 2 0, 1 0))",
    "POLYGON ((5 0, 5 1, 6 1, 6 0, 5 0))"
]

REGIONS_2 = [
    "POLYGON ((3 0, 3 1, 4 1, 4 0, 3 0))",
]

parser = argparse.ArgumentParser()
parser.add_argument('builder')
parser.add_argument('filenames', nargs='+')
arguments = parser.parse_args()

for filename, regions in zip(arguments.filenames, [REGIONS_1, REGIONS_2]):
    with open(filename, "w") as file:
        subprocess.run(
            [arguments.builder],
            input='\n'.join(regions),
            stdout=file
        )
