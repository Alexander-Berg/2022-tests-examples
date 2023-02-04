import pytest
import subprocess32 as subprocess
import yatest.common


@pytest.mark.parametrize(
    'input_filename,output_filename,binary_filename',
    [
        ('input.txt', 'output_ch.txt', 'ch_pipe/ch_pipe'),
        ('input.txt', 'output_yt.txt', 'yt_pipe/yt_pipe'),
    ]
)
def test_canonical(input_filename, output_filename, binary_filename):
    test_path = 'balancer/test/accesslog_converter_for_pushclient/'
    canonical_input = yatest.common.source_path(test_path + input_filename)
    canonical_output = yatest.common.source_path(test_path + output_filename)

    binary = yatest.common.binary_path(
        'balancer/production/x/accesslog_converter_for_pushclient/' + binary_filename
    )

    subprocess.check_call([binary], stdin=open(canonical_input, 'r'), stdout=open('real_output.txt', 'w'))
    subprocess.check_call(['diff', canonical_output, 'real_output.txt'], stdout=open('diff.patch', 'w'))
