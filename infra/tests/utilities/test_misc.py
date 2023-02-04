"""Test misc utilities."""
import sys
from collections import namedtuple, OrderedDict

import pytest
import six
from six import wraps

from walle.util.misc import args_as_dict, iter_shuffle, merge_iterators_by_uuid, fix_mongo_batch_update_dict


def execute_args_as_dict(func, *args, **kwargs):
    def decorating(params):
        def decorator(func):
            @wraps(func)
            def wrapped(*args, **kwargs):
                params.update(args_as_dict(func, *args, **kwargs))
                return func(*args, **kwargs)

            return wrapped

        return decorator

    func_args = {}
    decorating(func_args)(func)(*args, **kwargs)
    return func_args


class TestIterShuffle:
    def test_shuffle_of_empty_list_is_empty(self):
        assert list(iter_shuffle([])) == []

    def test_shuffle_of_one_element_list_is_same_as_original(self):
        assert list(iter_shuffle([1])) == [1]

    def test_shuffles_list_in_place(self):
        original_list = list(range(101))
        target_list = list(range(101))
        assert original_list == target_list

        shuffled_list = list(iter_shuffle(target_list))
        assert shuffled_list == shuffled_list
        # NB: although the probability is very slow (1/list_size!),
        # we can still have shuffled list exactly match the original one.
        assert target_list != original_list


class TestArgsAsDictUtil:
    def setup(self):
        args = OrderedDict()
        args["arg1"] = "this is arg 1"
        args["arg2"] = "this is arg 2"
        args["arg3"] = "this is arg 3"
        args["arg4"] = "this is arg 4"
        args["kwarg1"] = "this is kwarg 1"
        args["kwarg2"] = "this is kwarg 2"
        args["kwarg3"] = "this is kwarg 3"
        args["kwarg4"] = "this is kwarg 4"

        self.args = args

        def verify(expected, arg1, arg2, arg3, arg4, kwarg1, kwarg2, kwarg3, kwarg4):
            assert arg1 == expected["arg1"]
            assert arg2 == expected["arg2"]
            assert arg3 == expected["arg3"]
            assert arg4 == expected["arg4"]
            assert kwarg1 == expected["kwarg1"]
            assert kwarg2 == expected["kwarg2"]
            assert kwarg3 == expected["kwarg3"]
            assert kwarg4 == expected["kwarg4"]

        self.verify = verify

    def test_all_parameters_are_positional(self):
        # test all parameters passed as positional args
        assert execute_args_as_dict(self.verify, self.args, *self.args.values()) == dict(self.args, expected=self.args)

    def test_all_parameters_passed_as_kwargs(self):
        # test all parameters passed as kwargs
        def all_as_kwargs(arg1, arg2, **kwargs):
            self.verify(self.args, arg1, arg2, **kwargs)

        assert execute_args_as_dict(all_as_kwargs, **self.args) == self.args

    def test_all_parameters_passed_as_kwargs_and_some_have_defaults(self):
        # test all parameters passed as kwargs, function have defaults for one of them
        def kwargs_with_defaults(arg1, arg2, arg3="default value", **kwargs):
            self.verify(self.args, arg1, arg2, arg3, **kwargs)

        assert execute_args_as_dict(kwargs_with_defaults, **self.args) == self.args

    def test_all_parameters_passed_as_kwargs_some_missing_and_have_defaults(self):
        # test all parameters passed as kwargs, one parameter is missing, function have a default for it
        local_args = self.args.copy()
        del local_args["arg3"]
        local_result = dict(local_args, arg3="arg3 default value")

        def kwargs_with_missing(arg1, arg2, arg3="arg3 default value", **kwargs):
            self.verify(local_result, arg1, arg2, arg3, **kwargs)

        assert execute_args_as_dict(kwargs_with_missing, **local_args) == local_result

    def test_some_positional_parameters_have_defaults(self):
        # test all parameters passed as positional arguments, function have defaults for two of them
        positional_args = OrderedDict()
        positional_args["arg1"] = "this is arg 1"
        positional_args["arg2"] = "this is arg 2"
        positional_args["arg3"] = "this is arg 3"
        positional_args["arg4"] = "this is arg 4"
        kw_args = OrderedDict()
        kw_args["kwarg1"] = "this is kwarg 1"
        kw_args["kwarg2"] = "this is kwarg 2"
        kw_args["kwarg3"] = "this is kwarg 3"
        kw_args["kwarg4"] = "this is kwarg 4"
        local_args = dict(positional_args, **kw_args)

        def positional_with_defaults(arg1, arg2, arg3="default value", arg4=None, **kwargs):
            self.verify(local_args, arg1, arg2, arg3, arg4, **kwargs)

        assert execute_args_as_dict(positional_with_defaults, *positional_args.values(), **kw_args) == local_args

    def test_some_positional_parameters_missing_but_have_defaults(self):
        # test some positional parameters missing, but have default values
        positional_args = OrderedDict()
        positional_args["arg1"] = "this is arg 1"
        positional_args["arg2"] = "this is arg 2"
        positional_args["arg3"] = "this is arg 3"
        kw_args = OrderedDict()
        kw_args["kwarg1"] = "this is kwarg 1"
        kw_args["kwarg2"] = "this is kwarg 2"
        kw_args["kwarg3"] = "this is kwarg 3"
        kw_args["kwarg4"] = "this is kwarg 4"

        local_result = dict(positional_args, arg4="arg4 default value", **kw_args)

        def kwargs_with_missing(arg1, arg2, arg3, arg4="arg4 default value", **kwargs):
            self.verify(local_result, arg1, arg2, arg3, arg4, **kwargs)

        assert execute_args_as_dict(kwargs_with_missing, *positional_args.values(), **kw_args) == local_result

    def test_some_parameters_missing(self):
        # test all parameters passed as kwargs, one parameter is missing
        local_args = self.args.copy()
        del local_args["arg3"]

        def params_missing(arg1, arg2, arg3, arg4, **kwargs):
            self.verify({}, arg1, arg2, arg3, arg4, **kwargs)

        with pytest.raises(TypeError) as ex:
            execute_args_as_dict(params_missing, **local_args)

        expected_message = (
            "params_missing() takes exactly 4 arguments (3 given)"
            if six.PY2
            else "params_missing() missing 1 required positional argument: 'arg3'"
        )
        if sys.version_info >= (3, 10):
            expected_message = "TestArgsAsDictUtil.test_some_parameters_missing.<locals>." + expected_message

        assert expected_message == str(ex.value)


UuidElem = namedtuple("UuidElem", ("uuid",))


def test_merge_iterators_symmetrical():
    a = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]
    b = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]

    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (UuidElem(uuid="a"), UuidElem(uuid="a")),
        (UuidElem(uuid="b"), UuidElem(uuid="b")),
        (UuidElem(uuid="c"), UuidElem(uuid="c")),
    ]


def test_merge_iterators_symmetrical_fewer_a():
    a = [UuidElem(uuid="a"), UuidElem(uuid="b")]
    b = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]

    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (UuidElem(uuid="a"), UuidElem(uuid="a")),
        (UuidElem(uuid="b"), UuidElem(uuid="b")),
        (None, UuidElem(uuid="c")),
    ]


def test_merge_iterators_symmetrical_fewer_b():
    a = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]
    b = [UuidElem(uuid="a"), UuidElem(uuid="b")]

    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (UuidElem(uuid="a"), UuidElem(uuid="a")),
        (UuidElem(uuid="b"), UuidElem(uuid="b")),
        (UuidElem(uuid="c"), None),
    ]


def test_merge_iterators_random():
    a = [UuidElem(uuid="a"), UuidElem(uuid="c"), UuidElem(uuid="d")]
    b = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]

    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (UuidElem(uuid="a"), UuidElem(uuid="a")),
        (None, UuidElem(uuid="b")),
        (UuidElem(uuid="c"), UuidElem(uuid="c")),
        (UuidElem(uuid="d"), None),
    ]


def test_merge_iterators_full_asymmetrical_a_first():
    a = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]
    b = [UuidElem(uuid="d"), UuidElem(uuid="e"), UuidElem(uuid="f")]
    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (UuidElem(uuid="a"), None),
        (UuidElem(uuid="b"), None),
        (UuidElem(uuid="c"), None),
        (None, UuidElem(uuid="d")),
        (None, UuidElem(uuid="e")),
        (None, UuidElem(uuid="f")),
    ]


def test_merge_iterators_full_asymmetrical_b_first():
    a = [UuidElem(uuid="d"), UuidElem(uuid="e"), UuidElem(uuid="f")]
    b = [UuidElem(uuid="a"), UuidElem(uuid="b"), UuidElem(uuid="c")]
    assert list(merge_iterators_by_uuid(iter(a), iter(b))) == [
        (None, UuidElem(uuid="a")),
        (None, UuidElem(uuid="b")),
        (None, UuidElem(uuid="c")),
        (UuidElem(uuid="d"), None),
        (UuidElem(uuid="e"), None),
        (UuidElem(uuid="f"), None),
    ]


def test_fix_mongo_batch_update_dict_only_sets():
    source = {"$set": {"a": 1, "b": 2}}
    target = {"$set": {"a": 1, "b": 2}}
    assert target == fix_mongo_batch_update_dict(source)


def test_fix_mongo_batch_update_dict_some_unsets():
    source = {"$set": {"a": 1, "b": None}}
    target = {"$set": {"a": 1}, "$unset": {"b": True}}
    assert target == fix_mongo_batch_update_dict(source)
