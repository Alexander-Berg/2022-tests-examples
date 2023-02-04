import os
import tarfile

import yatest.common


def apply_bsdiff(input_file_tar, out_file):
    courgette_bsdiff = yatest.common.binary_path("contrib/tools/courgette_bsdiff/courgette_bsdiff")
    for entry in input_file_tar.getmembers():
        if not entry.name.endswith(".base"):
            continue

        base = entry.name
        patch = os.path.splitext(base)[0] + ".patch"

        base_dst = yatest.common.output_path("base.bin")
        patch_dst = yatest.common.output_path("patch.bin")
        decoded_dst = yatest.common.output_path("decoded.bin")

        with open(base_dst, "wb") as base_fd, open(patch_dst, "wb") as patch_fd:
            base_src = input_file_tar.extractfile(input_file_tar.getmember(base))
            patch_src = input_file_tar.extractfile(input_file_tar.getmember(patch))
            base_fd.write(base_src.read())
            patch_fd.write(patch_src.read())
            base_src.close()
            patch_src.close()

        yatest.common.execute(
            [courgette_bsdiff, "--applybsdiff", base_dst, decoded_dst, patch_dst],
            check_exit_code=True,
        )

        with open(decoded_dst, "rb") as decoded_fd:
            out_file.write(decoded_fd.read())


def test_with_courgette_bsdiff_tool():
    input_tar = "./bsdiff_test_data.tgz"
    out_file_path = "./sample.out"
    with open(out_file_path, "wb") as out_file, tarfile.open(input_tar, "r:gz") as input_file_tar:
        apply_bsdiff(input_file_tar, out_file)
    return yatest.common.canonical_file(out_file_path)
