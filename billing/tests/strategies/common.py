import string

from hypothesis import strategies as st

from ..utils import tz

__all__ = ['optional', 'ascii_text', 'non_null_text', 'utc_datetimes']


def optional(stat):
    return st.one_of(st.none(), stat)


def ascii_text(**kwargs):
    return st.text(list(string.ascii_lowercase), **kwargs)


def non_null_text(**kwargs):
    return st.text(alphabet=st.characters(blacklist_categories=('Cc', 'Cs')), **kwargs).filter(
        lambda x: not x.isspace()
    )


def utc_datetimes(**kwargs):
    return st.datetimes(**kwargs, timezones=st.just(tz.utc_tz()))
