# coding: utf-8
from balance import balance_steps as steps


def run(object_id):
    steps.CommonSteps.export('NIRVANA_BLOCK', 'NirvanaBlock', object_id)


if __name__ == '__main__':
    run(6008)
