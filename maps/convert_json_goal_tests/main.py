import getopt
import json
import sys


def main():
    args, _ = getopt.getopt(sys.argv[1:], 'i:o:')
    args = dict(args)

    if "-i" not in args or "-o" not in args:
        raise RuntimeError(
            "Usage: ./convert_json_goal_tests -i src_json -o output_file"
        )

    input_json_file = args.get("-i")
    output_file = args.get("-o")

    with open(input_json_file, "r") as f:
        input_json = json.load(f)

    output = []
    for el in input_json:
        output.append({
            "lang": el["lang"],
            "ll": el["ll"],
            "query": el["query"],
            "spn": el["spn"],
            "duplicates": (el["duplicates"] if ("duplicates" in el) else None),
            "response": "\n".join(el["response"]),
            "validate_set": el["validate_set"]
        })

    with open(output_file, "w") as f:
        f.write('\n'.join([json.dumps(el) for el in output]))
