import yt.wrapper

import maps.analyzer.pylibs.yql.params as params


def test_params():
    etalon = {
        '$null': params.ValueBuilder.make_null(),
        '$str': params.ValueBuilder.make_string('string'),
        '$path': params.ValueBuilder.make_string('<"columns"=["clid";];>//path'),
        '$yson': params.ValueBuilder.make_string('<"append"=%true;>//path'),
        '$int': params.ValueBuilder.make_int64(123),
        '$double': params.ValueBuilder.make_double(1.0),
        '$bool': params.ValueBuilder.make_bool(False),
        '$list': params.ValueBuilder.make_list([
            params.ValueBuilder.make_string('str'),
            params.ValueBuilder.make_bool(True),
        ]),
        '$dict': params.ValueBuilder.make_dict([
            (params.ValueBuilder.make_string('x'), params.ValueBuilder.make_string('foo')),
        ]),
        '$tuple': params.ValueBuilder.make_tuple([
            params.ValueBuilder.make_string('str'),
            params.ValueBuilder.make_bool(True),
        ]),
        '$struct': params.ValueBuilder.make_struct(
            name=params.ValueBuilder.make_string('Ivan'),
            age=params.ValueBuilder.make_int64(33),
        ),
        '$optional_list': params.ValueBuilder.make_list([
            params.ValueBuilder.make_list([
                params.ValueBuilder.make_int64(10),
            ])
        ]),
        '$optional_null': params.ValueBuilder.make_null(),
    }
    result = params.create_params(
        null=None,
        str='string',
        path=yt.wrapper.ypath.TablePath('//path', columns=['clid']),
        yson=yt.wrapper.ypath.YPath('//path', attributes={'append': True}),
        int=123,
        double=1.0,
        bool=False,
        list=['str', True],
        dict={'x': 'foo'},
        tuple=('str', True),
        struct=params.Struct(name='Ivan', age=33),
        optional_list=params.optional([10]),
        optional_null=params.optional(None),
    )
    assert params.build_params(etalon) == result
