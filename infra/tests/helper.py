import copy
import pprint
import timeit

import pytest

from ya.skynet.services.heartbeatserver.bulldozer import helper

from . import context

ctx = context.Context(context.Context.Config({}))


@pytest.mark.new
def test_fix_keys():
    struct = [
        'foo', 'bar', [
            'zoo', {
                'goodkey': [
                    {
                       'bad.key.one': {
                           'bad$key$two': 2
                       }
                    },
                    1,
                    {
                       'bad.key$three': 3
                    },
                ],
                'bad.key$four': [1, {'bad$key.five': 5}, 3]
            },
        ],
    ]

    nstruct = helper.fixKeys(copy.deepcopy(struct), to=u'__')
    ctx.log.debug('New structure:\n%s', pprint.pformat(nstruct))

    assert nstruct != struct
    assert nstruct[2][1]['goodkey'][0]['bad_key_one']['bad_key_two'] == 2
    assert nstruct[2][1]['goodkey'][2]['bad_key_three'] == 3
    assert nstruct[2][1]['bad_key_four'][1]['bad_key_five'] == 5

    ITERATIONS = 25000
    ctx.log.info('Performance test: running %d iterations...', ITERATIONS)
    dur = timeit.Timer(lambda: helper.fixKeys(nstruct)).timeit(ITERATIONS)
    ctx.log.info('%d iterations done in %.5fs (%.2f iterations per second).', ITERATIONS, dur, ITERATIONS / dur)
