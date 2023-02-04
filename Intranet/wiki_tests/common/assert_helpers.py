class InAnyOrder:
    def __init__(self, lst):
        self.lst = lst


def in_any_order(iterable):
    return InAnyOrder(iterable)


def assert_json(dut, expected_result, depth='root'):
    """
    Сравним только те поля которые есть в эталоне и игнорируем прочие
    """

    if isinstance(expected_result, dict):
        for k, v in expected_result.items():
            assert k in dut, f'Dict at {depth}: expected no key {v} in {dut}'
            assert_json(dut[k], v, depth=f'{depth}.{k}')

    elif isinstance(expected_result, list):
        assert isinstance(dut, list), f'List at {depth}: {dut} is not a list'
        assert len(dut) == len(
            expected_result
        ), f'List at {depth}: expected length {len(expected_result)}, got {len(dut)}\n value {dut}'

        for k, v in enumerate(expected_result):
            assert_json(dut[k], v, depth=f'{depth}[{k}]')

    elif isinstance(expected_result, InAnyOrder):
        assert isinstance(dut, list), f'List at {depth}: {dut} is not a list'
        assert len(dut) == len(
            expected_result.lst
        ), f'List at {depth}: expected length {len(expected_result.lst)}, got {len(dut)}\n value {dut}'

        matched = set()
        for k, dut_item in enumerate(dut):
            found = False
            for k2, v_2 in enumerate(expected_result.lst):
                if k2 in matched:
                    continue
                try:
                    assert_json(dut_item, v_2, depth=f'{depth}[{k}]')
                    found = True
                    matched.add(k2)
                    break
                except AssertionError:
                    continue

            if not found:
                assert False, f'List at {depth}: Element {dut_item} has no match'

    else:
        assert dut == expected_result, f'Value at {depth}: expected {expected_result}, got {dut}'
