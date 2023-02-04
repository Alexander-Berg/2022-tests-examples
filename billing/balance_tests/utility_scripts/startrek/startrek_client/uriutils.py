# coding: utf-8

import re

from six.moves.urllib.parse import urlencode


try:
    from urllib.parse import quote
except ImportError:
    from urllib import quote

RESERVED = ":/?#[]@!$&'()*+,;="
VARIABLE = re.compile(r'{([\w\d\-_\.]+)}')
TEMPLATE = re.compile(r'{([^\}]+)}')
OPERATOR = "+#./;?&|!@"


class Matcher(object):

    def __init__(self):
        self._patterns = []

    def add(self, uri, resource, priority=0):
        parts = uri.strip('/').split('/')

        pattern_parts = []
        for part in parts:
            is_variable = VARIABLE.search(part)
            if is_variable:
                pattern_part = r'(?P<{0}>[\w\d\-\_\.]+)'.format(
                    is_variable.group(1)
                )
                pattern_parts.append(pattern_part)
            else:
                pattern_parts.append(part)

        pattern = re.compile('/'.join(pattern_parts))
        self._patterns.append((
            priority,
            pattern,
            resource
        ))

        #sort by priority
        self._patterns.sort(key=lambda it: it[0], reverse=True)  # ok for our N < 20

    def match(self, uri):
        path = uri.strip('/')
        for _, pattern, value in self._patterns:
            match = pattern.match(path)
            if match:
                return value
        return None


def _quote(value, safe, prefix=None):
    if prefix is not None:
        return quote(str(value)[:prefix], safe)
    return quote(str(value), safe)


def _tostring(varname, value, explode, prefix, operator, safe=""):
    if isinstance(value, list):
        return ",".join(_quote(x, safe) for x in value)
    if isinstance(value, dict):
        keys = sorted(value.keys())
        if explode:
            return ",".join(
                _quote(key, safe) + "=" + _quote(value[key], safe)
                for key in keys
            )
        else:
            return ",".join(
                _quote(key, safe) + "," + _quote(value[key], safe)
                for key in keys
            )
    elif value is None:
        return
    else:
        return _quote(value, safe, prefix)


def _tostring_path(varname, value, explode, prefix, operator, safe=""):
    joiner = operator
    if isinstance(value, list):
        if explode:
            out = [_quote(x, safe) for x in value if value is not None]
        else:
            joiner = ","
            out = [_quote(x, safe) for x in value if value is not None]
        if out:
            return joiner.join(out)
        else:
            return
    elif isinstance(value, dict):
        keys = sorted(value.keys())
        if explode:
            out = [
                _quote(key, safe) + "=" + _quote(value[key], safe)
                for key in keys if value[key] is not None
            ]
        else:
            joiner = ","
            out = [
                _quote(key, safe) + "," + _quote(value[key], safe)
                for key in keys if value[key] is not None
            ]
        if out:
            return joiner.join(out)
        else:
            return
    elif value is None:
        return
    else:
        return _quote(value, safe, prefix)


def _tostring_semi(varname, value, explode, prefix, operator, safe=""):
    joiner = operator
    if operator == "?":
        joiner = "&"
    if isinstance(value, list):
        if explode:
            out = [
                varname + "=" + _quote(x, safe)
                for x in value if x is not None
            ]
            if out:
                return joiner.join(out)
            else:
                return
        else:
            return varname + "=" + ",".join(_quote(x, safe) for x in value)
    elif isinstance(value, dict):
        keys = sorted(value.keys())
        if explode:
            return joiner.join([
                _quote(key, safe) + "=" + _quote(value[key], safe)
                for key in keys if key is not None
            ])
        else:
            return varname + "=" + ",".join([
                _quote(key, safe) + "," + _quote(value[key], safe)
                for key in keys if key is not None
            ])
    else:
        if value is None:
            return
        elif value:
            return varname + "=" + _quote(value, safe, prefix)
        else:
            return varname


def _tostring_query(varname, value, explode, prefix, operator, safe=""):
    joiner = operator
    if operator in ["?", "&"]:
        joiner = "&"
    if isinstance(value, list) or isinstance(value, tuple):
        if 0 == len(value):
            return None
        if explode:
            return joiner.join(varname + "=" + _quote(x, safe) for x in value)
        else:
            return varname + "=" + ",".join(_quote(x, safe) for x in value)

    elif isinstance(value, dict):
        if 0 == len(value):
            return None
        keys = sorted(value.keys())
        if explode:
            return joiner.join(
                _quote(key, safe) + "=" + _quote(value[key], safe)
                for key in keys
            )
        else:
            return varname + "=" + ",".join(
                _quote(key, safe) + "," + _quote(value[key], safe)
                for key in keys
            )
    else:
        if value is None:
            return
        elif value:
            return varname + "=" + _quote(value, safe, prefix)
        else:
            return varname + "="

TOSTRING = {
    "": _tostring,
    "+": _tostring,
    "#": _tostring,
    ";": _tostring_semi,
    "?": _tostring_query,
    "&": _tostring_query,
    "/": _tostring_path,
    ".": _tostring_path,
}


def expand(template, variables=None):
    """
    Expand template as a URI Template using variables.
    """
    variables = variables or {}

    def _sub(match):
        expression = match.group(1)
        operator = ""
        if expression[0] in OPERATOR:
            operator = expression[0]
            varlist = expression[1:]
        else:
            varlist = expression

        safe = ""
        if operator in ["+", "#"]:
            safe = RESERVED
        varspecs = varlist.split(",")
        varnames = []
        defaults = {}
        for varspec in varspecs:
            default = None
            explode = False
            prefix = None
            if "=" in varspec:
                varname, default = tuple(varspec.split("=", 1))
            else:
                varname = varspec
            if varname[-1] == "*":
                explode = True
                varname = varname[:-1]
            elif ":" in varname:
                try:
                    prefix = int(varname[varname.index(":")+1:])
                except ValueError:
                    raise ValueError(u"non-integer prefix '{0}'".format(
                        varname[varname.index(":")+1:]))
                varname = varname[:varname.index(":")]
            if default:
                defaults[varname] = default
            varnames.append((varname, explode, prefix))

        retval = []
        joiner = operator
        start = operator
        if operator == "+":
            start = ""
            joiner = ","
        if operator == "#":
            joiner = ","
        if operator == "?":
            joiner = "&"
        if operator == "&":
            start = "&"
        if operator == "":
            joiner = ","
        for varname, explode, prefix in varnames:
            if varname in variables:
                value = variables[varname]
                if not value and value != "" and varname in defaults:
                    value = defaults[varname]
            elif varname in defaults:
                value = defaults[varname]
            else:
                continue
            expanded = TOSTRING[operator](
              varname, value, explode, prefix, operator, safe=safe)
            if expanded is not None:
                retval.append(expanded)
        if len(retval) > 0:
            return start + joiner.join(retval)
        else:
            return ""

    return TEMPLATE.sub(_sub, template)

def build(uri, params):
    if not params:
        return uri
    encoded = urlencode(params)
    if "?" in uri:
        return uri + "&" + encoded
    else:
        return uri + "?" + encoded
