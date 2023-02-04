import yatest.common


def test_changelog():
    binary = yatest.common.binary_path('infra/infractl/ci_tasklets/get_changelog/get_changelog')
    with open(yatest.common.source_path('infra/infractl/ci_tasklets/get_changelog/input.example.json')) as f:
        input_data = f.read()

    return yatest.common.canonical_execute(
        binary,
        save_locally=True,
        args=['run', '--test', 'GetChangelog', '--input', input_data],
        env={
            'LOGS_DIR': yatest.common.test_output_path(),
        },
    )
