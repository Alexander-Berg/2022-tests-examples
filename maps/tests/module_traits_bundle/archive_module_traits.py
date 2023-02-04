#!/usr/bin/env python
import argparse
import os
import tarfile


def main():
    parser = argparse.ArgumentParser(
        description="Creates a tar-file with input module_traits.json files")
    parser.add_argument(
        "-i",
        dest="input_files",
        nargs='+',
        metavar="INPUT_FILE",
        required=True)
    parser.add_argument(
        "-o",
        dest="output_file",
        metavar="OUTPUT_FILE",
        required=True)
    parser.add_argument(
        "--src-dir",
        dest="source_directory",
        metavar="SOURCE_DIRECTORY",
        required=True)
    arguments = parser.parse_args()

    output_dir = os.path.dirname(arguments.output_file)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    # Given a list of files "{path}/{module}/module_traits.json", create a tarball with
    # files named "{module}".
    with tarfile.open(arguments.output_file, "w") as tar:
        for file_name in arguments.input_files:
            module_traits_file = os.path.basename(file_name)
            module_name = os.path.basename(os.path.dirname(file_name))
            input_file = os.path.join(arguments.source_directory, module_name, module_traits_file)
            tar.add(input_file, arcname=module_name)  # arcname is for name in archive, not for arc VCS =)


if __name__ == "__main__":
    main()
