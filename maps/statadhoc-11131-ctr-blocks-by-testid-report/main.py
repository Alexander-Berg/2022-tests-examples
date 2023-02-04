#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
from collections import defaultdict
from nile.api.v1 import (
    cli,
    files,
    Record,
    statface as ns,
    aggregators as na,
    extractors as ne,
    with_hints,
    multischema,
    extended_schema,
    modified_schema,
)

from qb2.api.v1 import (
    extractors as qe,
    filters as qf,
    resources as qr,
)
from nile.utils.misc import safediv
from library.python import resource
import yaml

STATFACE_REPORT_BYAK = ns.StatfaceReport.from_dict_config(
    yaml.safe_load(resource.find('statadhoc-11131-ctr-blocks-by-testid-report/publish.yaml'))) \
    .path('Maps/Adhoc/CTRBlocksByTestID') \
    .title('CTR блоков Карт по экспериментам (bebr)')

STATFACE_REPORT_TOUCH = ns.StatfaceReport.from_dict_config(
    yaml.safe_load(resource.find('statadhoc-11131-ctr-blocks-by-testid-report/publish.yaml'))) \
    .path('Maps_Mobile_All/Adhoc/CTRBlocksByTestID') \
    .title('CTR блоков бякотач по экспериментам (bebr)')

TOUCH = '235'
BYAK = '30'

MAX_BLOCKS = 50000
EVENTS = {
    'show': '0-show',
    'click': '1-click'
}


def extract_experiments(test_buckets, geo_experiments, white):
    experiments = set()
    tests = (test_buckets or '').split(';')
    for test in tests:
        test_components = test.split(',')
        if len(test_components) == 3:
            try:
                test_id = int(test_components[0])  # synonym - exp_id
                if (str(test_id) in white or
                        test_id in geo_experiments.get('Maps', []) or
                        test_id in geo_experiments.get('Maps_Plus_Beta', [])):
                    experiments.add(str(test_id))
            except ValueError:
                pass

    if not experiments:
        experiments.add('_not_experiment_')

    return list(experiments)


def generate_paths(path, block_vars):
    yield path

    if not block_vars:
        return

    path += ('_vars_',)
    yield path

    for var_key, var_value in block_vars.iteritems():
        try:
            var_key = var_key.encode('utf8')
            yield path + (var_key,)
        except UnicodeDecodeError:
            pass

        if isinstance(var_value, unicode):
            var_value = var_value.encode('utf8')
        else:
            var_value = str(var_value)

        if len(var_value) > 100:
            for i in range(50):
                try:
                    var_value[: 50 + i].decode('utf8')
                except UnicodeDecodeError:
                    pass
                else:
                    var_value = var_value[: 50 + i] + '<...>'
                    break
        var_value = ' '.join(var_value.split('\t'))
        try:
            json.loads(json.dumps(var_value))  # fix: https://st.yandex-team.ru/MAMA-1003
            yield path + (var_key, var_value)
        except ValueError as e:
            if not (e.message and (e.message.startswith('Unpaired low surrogate:')
                                   or e.message.startswith('Unpaired high surrogate:'))):
                raise e


@with_hints(output_schema=dict(
    click=int,
    show=int,
    session=int,
    clicked_session=int,
    experiments=[str],
    path=str,
    pid=str,
    yandexuid=str,
    log_date=str,
))
def reduce_sessions(groups):
    for key, records in groups:
        experiments = set(['_total_'])
        blocks = dict()
        values = defaultdict(lambda: defaultdict(int))
        for record in records:
            if len(blocks) > MAX_BLOCKS:
                continue
            experiments.update(record.experiments)
            path = ('R',) + tuple(record.path.split('.')[1:])
            blocks[record.block_id] = (record.parent_id, path)

            parent_id = record.parent_id

            for sub_path in generate_paths(path, record.vars):
                values[sub_path][record.event_type] += 1

            if record.event_type != 'click':
                continue

            while parent_id:
                parent_id, parent_path = blocks.get(parent_id, (None, None))
                if parent_path:
                    values[parent_path]['click'] += 1

        if len(blocks) > MAX_BLOCKS:
            continue

        for path, events in values.iteritems():
            sub_values = {
                'path': json.dumps(path),
                'yandexuid': key.yandexuid,
                'log_date': key.log_date,
                'experiments': list(experiments),
                'session': 1,
                'clicked_session': 1 if events['click'] else 0,
                'pid': key.pid
            }
            events.update(sub_values)
            for field in ['show', 'click', 'session']:
                events[field] = events[field] or 0

            yield Record.from_dict(dict(events))


def tree_view(path):
    return '\t'.join([i or '_empty_' for i in path])


@with_hints(output_schema=modified_schema(
    extend=dict(
        label=str,
        sub_path=str,
        sessions=int,
        clicked_sessions=int,
        visitors=int,
        clicked_visitors=int,
        shows=int,
        clicks=int,
    ),
    exclude=[
        'yandexuid',
    ]
))
def label_blocks(groups):
    for key, records in groups:
        user_values = {
            'sessions': 0,
            'clicked_sessions': 0,
            'visitors': 0,
            'clicked_visitors': 0,
            'shows': 0,
            'clicks': 0
        }
        for record in records:
            for value in user_values:
                user_values[value] += record.get(value, 0)

        raw_path = json.loads(key.path)

        path = '\t' + tree_view(raw_path) + '\t'
        sub_path = '\t' + tree_view(raw_path[:-1]) + '\t'

        user_values.update({
            'pid': key.pid,
            'path': path,
            'sub_path': sub_path,
            'experiment': key.experiment,
            'fielddate': key.fielddate,
            'label': 'all'
        })
        if len(raw_path) >= 3 and raw_path[-3] == '_vars_':
            user_values['label'] = 'value'
            user_values['path'] = sub_path
            user_values['sub_path'] = path

        yield Record.from_dict(user_values)


@with_hints(output_schema=extended_schema())
def fold_vars(groups):
    for key, records in groups:
        parent_record = next(records)
        yield parent_record
        for record in records:
            if safediv(record.shows, parent_record.shows) < 0.005 \
                    and safediv(record.clicks, parent_record.clicks) < 0.005:
                continue
            cur_record = record.to_dict()
            cur_record['path'] = record.sub_path
            yield Record.from_dict(cur_record)


@with_hints(output_schema=multischema(extended_schema(), extended_schema()))
def split_by_project(records, stream_byak, stream_touch):
    stream_by_pid = {
        BYAK: stream_byak,
        TOUCH: stream_touch,
    }

    for record in records:
        stream_by_pid[record.pid](record)


@cli.statinfra_job
def make_job(job, options, statface_client):
    job = job.env(
        yt_spec_defaults=dict(
            pool_trees=["physical"],
            tentative_pool_trees=["cloud"]
        )
    )

    statface_report_byak = STATFACE_REPORT_BYAK \
        .scale(options.scale) \
        .client(statface_client) \
        .replace_mask('fielddate')

    statface_report_touch = STATFACE_REPORT_TOUCH \
        .scale(options.scale) \
        .client(statface_client) \
        .replace_mask('fielddate')

    report = job.table(
        '//home/maps/analytics/logs/cooked-bebr-log/{desktop-maps,touch-maps}/{clean,fraud}/@dates') \
        .project(
        'yandexuid', 'session_id', 'block_id', 'parent_id',
        'path', 'vars', 'event_type', 'pid', 'ip', 'log_date',
        qe.custom(
            'experiments',
            extract_experiments,
            'test_buckets',
            qr.yaml('geo-experiments.yaml'),
            qr.yaml('geo-experiments-whitelist.yaml'),
        ).allow_null_dependency().with_type([str]),
        qe.yql_custom('type', "IF($p0=='show', '0-show', IF($p0=='click', '1-click', ''))", 'event_type'),
        files=[
            files.StatboxDict('geo-experiments.yaml', use_latest=True),
            files.StatboxDict('geo-experiments-whitelist.yaml')
        ],
        intensity=1.0,
    ) \
        .filter(
        qf.defined('session_id', 'yandexuid', 'path', 'ip'),
        qf.one_of('event_type', {'show', 'click'}),
        intensity=0.4,
    ) \
        .groupby('yandexuid', 'session_id', 'pid', 'log_date') \
        .sort('type') \
        .reduce(reduce_sessions, intensity=0.7) \
        .project(
        'click',
        'session',
        'show',
        'clicked_session',
        'pid',
        'path',
        'yandexuid',
        qe.unfold('experiment', 'experiments').with_type(str),
        fielddate='log_date',
        intensity=1.0) \
        .groupby('yandexuid', 'experiment', 'path', 'pid', 'fielddate') \
        .aggregate(
        sessions=na.sum('session'),
        clicked_sessions=na.sum('clicked_session'),
        shows=na.sum('show'),
        clicks=na.sum('click'),
        intensity=1.0) \
        .project(
        ne.all(),
        qe.yql_custom('visitors', 'IF($p0>0, 1, 0)', 'shows'),
        qe.yql_custom('clicked_visitors', 'IF($p0>0, 1, 0)', 'clicks'),
        intensity=0.1,
    ) \
        .groupby('fielddate', 'experiment', 'path', 'pid') \
        .reduce(label_blocks, intensity=0.5) \
        .groupby('fielddate', 'experiment', 'path', 'pid') \
        .sort('label') \
        .reduce(fold_vars)

    report_byak, report_touch = report.map(split_by_project)

    report_byak \
        .put('$job_root/byak/$date') \
        .publish(statface_report_byak, remote_mode=True)

    report_touch \
        .put('$job_root/touch/$date') \
        .publish(statface_report_touch, remote_mode=True)

    return job


def main():
    cli.run()


if __name__ == '__main__':
    main()
