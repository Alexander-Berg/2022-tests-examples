from maps.infra.conserva.lib.options import S3Options, YTOptions
from maps.infra.conserva.lib.yt_provider import YT
from maps.infra.conserva.lib.s3_provider import S3
from maps.infra.conserva.lib.constants import MiB


def _create_file(symbol: str, size: int):
    data = symbol * size
    filename = 'file.tmp'
    with open(filename, 'wb') as file:
        file.write(data.encode())
    return filename


def _check_files(s3: S3, symbol: str, size: int):
    real_size = 0
    for chunk in s3.read_file_chunks(symbol):
        real_size += len(chunk)
        assert symbol.encode()[0] in chunk
    assert size == real_size


def create_backup(yt: YT, s3: S3, s3_options: S3Options):
    meta_table = 'meta'
    old_files = yt.fetch_meta(meta_table=meta_table)
    new_files_meta = s3.read_file_sizes(ignore=set(old_files.keys()))
    if new_files_meta:
        yt.append_files_meta(meta_table=meta_table, meta=new_files_meta)
    yt.run_backup(meta_table, 3, s3_options)


def run_synthetic_backup_restore(s3_options: S3Options, yt_options: YTOptions) -> None:
    s3 = S3(s3_options)
    yt = YT(yt_options)
    s3.cleanup_bucket()
    yt.cleanup()

    first_params = (('a', 2 * MiB), ('b', 50 * MiB), ('c', 1 * MiB))
    second_params = (('d', 2 * MiB), ('e', 50 * MiB), ('f', 1 * MiB))

    for symbol, size in first_params:
        s3.push_file(symbol, _create_file(symbol, size))
    create_backup(yt, s3, s3_options)

    for symbol, size in second_params:
        s3.push_file(symbol, _create_file(symbol, size))
    create_backup(yt, s3, s3_options)

    s3.cleanup_bucket()
    assert not s3.read_file_sizes()

    yt.restore_backup(set(), s3_options, 5, 10)

    for symbol, size in first_params:
        _check_files(s3, symbol, size)

    for symbol, size in second_params:
        _check_files(s3, symbol, size)
