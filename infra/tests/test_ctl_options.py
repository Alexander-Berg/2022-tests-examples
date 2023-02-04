import os
import json
import click

import typing
from click.testing import CliRunner
from google.protobuf import json_format
from infra.qyp.vmctl.src import ctl_options, common


def call_options_cls(options_cls, *args, **kwargs):  # type: (...) -> (int, typing.Union[dict, str])
    @click.command()
    @options_cls.decorate
    def command(**command_kwargs):
        _args = options_cls.build_from_kwargs(command_kwargs)
        print json.dumps(_args.command_options)

    runner = CliRunner()
    res = runner.invoke(command, *args, **kwargs)

    return res.exit_code, json.loads(res.output) if res.exit_code == 0 else res.output or res.exc_info


def test_pod_id_type():
    class Test(common.CommandOptions):
        pod_id = common.OptionValue('--pod-id', type=ctl_options.PodIdType(), help='test')

    exit_code, output = call_options_cls(Test, ['--pod-id', 'HALLOW'])

    assert exit_code != 0
    assert "Invalid value for '--pod-id': HALLOW should match the pattern" in output

    exit_code, output = call_options_cls(Test, ['--pod-id', 'hallow-test'])
    assert exit_code == 0
    assert output['pod_id'] == 'hallow-test'


def test_network_id_type():
    class Test(common.CommandOptions):
        network_id = common.OptionValue('--network-id', type=ctl_options.NetworkIdType(), help='test')

    exit_code, output = call_options_cls(Test, ['--network-id', 'HALLOW'])

    assert exit_code != 0
    assert 'Invalid value for \'--network-id\': Invalid racktables macro "HALLOW", should match the pattern:' in output

    exit_code, output = call_options_cls(Test, ['--network-id', '_SS_'])
    assert exit_code == 0, output
    assert output['network_id'] == '_SS_'


def test_size_type():
    class Test(common.CommandOptions):
        size = common.OptionValue('--size', type=ctl_options.SizeSuffixStringType(), help='test')

    exit_code, output = call_options_cls(Test, ['--size', 'sdfasdf'])
    assert exit_code != 0
    assert 'Invalid size value "sdfasdf", valid suffixes are Kk, Mm, Gg and Tt or none' in output

    exit_code, output = call_options_cls(Test, ['--size', '1'])
    assert exit_code == 0
    assert output['size'] == 1

    exit_code, output = call_options_cls(Test, ['--size', '1K'])
    assert exit_code == 0
    assert output['size'] == 1 * 1024

    exit_code, output = call_options_cls(Test, ['--size', '1M'])
    assert exit_code == 0
    assert output['size'] == 1 * 1024 * 1024

    exit_code, output = call_options_cls(Test, ['--size', '1G'])
    assert exit_code == 0
    assert output['size'] == 1 * 1024 * 1024 * 1024

    exit_code, output = call_options_cls(Test, ['--size', '1T'])
    assert exit_code == 0
    assert output['size'] == 1 * 1024 * 1024 * 1024 * 1024


def test_repeated_string_type():
    class Test(common.CommandOptions):
        strings = common.OptionValue('--strings',
                                     type=ctl_options.RepeatedString(),
                                     cls=common.OptionEatAll,
                                     default='default',
                                     envvar='ENVVAR_FOR_TESTING',
                                     help='test')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0
    assert output['strings'] == ['default']

    exit_code, output = call_options_cls(Test, ['--strings', 'h'])
    assert exit_code == 0
    assert output['strings'] == ['h']

    exit_code, output = call_options_cls(Test, ['--strings', 'hh'])
    assert exit_code == 0
    assert output['strings'] == ['hh']

    exit_code, output = call_options_cls(Test, ['--strings', 'hh', 'hh'])
    assert exit_code == 0
    assert output['strings'] == ['hh', 'hh']

    os.environ.setdefault('ENVVAR_FOR_TESTING', 'from_env_one')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0
    assert output['strings'] == ['from_env_one']

    del os.environ['ENVVAR_FOR_TESTING']

    os.environ.setdefault('ENVVAR_FOR_TESTING', 'from_env  from_env2 from_env3')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0
    assert output['strings'] == ['from_env', 'from_env2', 'from_env3']
    del os.environ['ENVVAR_FOR_TESTING']


def test_repeated_choices_type():
    CHOICES = ['one', '2', 'three']

    class Test(common.CommandOptions):
        choices = common.OptionValue('--choices',
                                     type=ctl_options.RepeatedChoices(CHOICES),
                                     cls=common.OptionEatAll,
                                     default=['one', '2'],
                                     envvar='ENVVAR_FOR_TESTING',
                                     help='test')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0, output
    assert output['choices'] == ['one', '2']

    exit_code, output = call_options_cls(Test, ['--choices', 'test'])
    assert exit_code != 0
    assert "Invalid value for '--choices': Invalid value \"test\", should in ['one', '2', 'three']" in output

    exit_code, output = call_options_cls(Test, ['--choices', 'one', 'test'])
    assert exit_code != 0
    assert "Invalid value for '--choices': Invalid value \"test\", should in ['one', '2', 'three']" in output

    exit_code, output = call_options_cls(Test, ['--choices', 'one'])
    assert exit_code == 0
    assert output['choices'] == ['one']

    exit_code, output = call_options_cls(Test, ['--choices', 'one', '2', 'three'])
    assert exit_code == 0
    assert output['choices'] == ['one', '2', 'three']

    os.environ.setdefault('ENVVAR_FOR_TESTING', 'one')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0
    assert output['choices'] == ['one']

    del os.environ['ENVVAR_FOR_TESTING']

    os.environ.setdefault('ENVVAR_FOR_TESTING', 'one  2 three')

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0
    assert output['choices'] == ['one', '2', 'three']
    del os.environ['ENVVAR_FOR_TESTING']


def test_extra_volumes_type():
    class Test(common.CommandOptions):
        extra_volume_list = common.OptionValue('--extra-volume',
                                               type=ctl_options.ExtraVolumeType(),
                                               multiple=True,
                                               order=1000,
                                               help='Disk volume options')

        def validate(self):
            self.extra_volume_list = [json_format.MessageToDict(x) for x in self.extra_volume_list]

    exit_code, output = call_options_cls(Test, [])
    assert exit_code == 0, output
    assert output['extra_volume_list'] == []

    # test ok, single
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,size=10G,storage=ssd'])
    assert exit_code == 0, output
    assert output['extra_volume_list'] == [
        {
            'name': 'disk1',
            'capacity': '10737418240',
            'imageType': 'RAW',
            'storageClass': 'ssd'
        }
    ]
    # test ok, multiple
    exit_code, output = call_options_cls(Test, ['--extra-volume',
                                                'name=disk1,size=10G,storage=ssd',
                                                '--extra-volume',
                                                'name=disk2,size=100G,storage=hdd,image=rbtorrent:12345'])
    assert exit_code == 0, output
    assert output['extra_volume_list'] == [
        {
            'name': 'disk1',
            'capacity': '10737418240',
            'imageType': 'RAW',
            'storageClass': 'ssd',
        },
        {
            'name': 'disk2',
            'capacity': '107374182400',
            'imageType': 'RAW',
            'storageClass': 'hdd',
            'resourceUrl': 'rbtorrent:12345',
        },
    ]
    # test invalid value format
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name='])
    assert exit_code == 2
    # test invalid value, duplicate keys
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,name=disk2,size=10G,storage=ssd'])
    assert exit_code == 2
    # test no required field name
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'size=10G,storage=ssd'])
    assert exit_code == 2
    # test no required field size
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,storage=ssd'])
    assert exit_code == 2
    # test no required field storage
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,size=10G'])
    assert exit_code == 2
    # test incorrect size
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,size=GGG,storage=ssd'])
    assert exit_code == 2
    # test incorrect storage
    exit_code, output = call_options_cls(Test, ['--extra-volume', 'name=disk1,size=10G,storage=place'])
    assert exit_code == 2


def test_extra_volumes_conf_type():
    class Test(common.CommandOptions):
        extra_volumes_conf = common.OptionValue('--extra-volumes-conf',
                                                type=ctl_options.ExtraVolumeConfType(),
                                                order=1000,
                                                help='Disk volume options')

        def validate(self):
            if self.extra_volumes_conf:
                self.extra_volumes_conf = [json_format.MessageToDict(x) for x in self.extra_volumes_conf]

    FILENAME = 'disk.yaml'

    # test ok
    content = """
    volumes:
      - storage: ssd
        name: disk1
        size: 1000G
      - name: disk2
        size: 100G
        storage: hdd
        image: rbtorrent:12345
    """
    with open(FILENAME, 'w') as f:
        f.write(content)
    exit_code, output = call_options_cls(Test, ['--extra-volumes-conf', FILENAME])
    assert exit_code == 0, output
    assert output['extra_volumes_conf'] == [
        {
            'name': 'disk1',
            'capacity': '1073741824000',
            'imageType': 'RAW',
            'storageClass': 'ssd',
        },
        {
            'name': 'disk2',
            'capacity': '107374182400',
            'imageType': 'RAW',
            'storageClass': 'hdd',
            'resourceUrl': 'rbtorrent:12345',
        },
    ]

    # test parse failed
    content = """
    volumes:
      ---- storage: ssd
        name: disk1
        size: 1000G
    """
    with open(FILENAME, 'w') as f:
        f.write(content)
    exit_code, output = call_options_cls(Test, ['--extra-volumes-conf', FILENAME])
    assert exit_code == 2

    # test empty file
    content = ''
    with open(FILENAME, 'w') as f:
        f.write(content)
    exit_code, output = call_options_cls(Test, ['--extra-volumes-conf', f.name])
    assert exit_code == 2

    # test empty volumes
    content = """
    volumes:
    """
    with open(FILENAME, 'w') as f:
        f.write(content)
    exit_code, output = call_options_cls(Test, ['--extra-volumes-conf', f.name])
    assert exit_code == 2
