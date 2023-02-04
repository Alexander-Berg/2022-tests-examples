# -*- coding: utf-8 -*-
import difflib
import os.path as path

import allure
import pytest
from enum import Enum
from hamcrest import assert_that, equal_to, calling, raises

from bfop import fop
from . import conf


class XslPath(object):
    DEFAULT = path.realpath("./fop-resources/xsl/main.xsl")
    RIT = path.realpath("./fop-resources/xsl/rit/rit.xsl")


class AttachmentType(Enum):
    def __init__(self, mime_type, extension, output_format):
        self.mime_type = mime_type
        self.extension = extension
        self.output_format = output_format

    @classmethod
    def get_by_output_format(cls, output_format):
        for name, member in cls.__members__.iteritems():
            if output_format == member.output_format:
                return member

        raise KeyError("Unknown format: {}".format(output_format))

    PDF = ("application/pdf", "pdf", fop.OutputFormat.PDF)
    RTF = ("application/msword", "rtf", fop.OutputFormat.RTF)
    XML = ("application/xml", "xml", fop.OutputFormat.FO_OUT)


def _read_file(filename):
    return open(filename, "rb").read()


def _get_diff(xml_str1, xml_str2):
    import lxml.etree as etree

    diff = difflib.context_diff(
        etree.tostring(etree.fromstring(xml_str1), pretty_print=True).splitlines(1),
        etree.tostring(etree.fromstring(xml_str2), pretty_print=True).splitlines(1),
    )

    return "".join(diff)


def _render(xml_name, xsl_path, output_format):
    with allure.step("Rendering {} with stylesheet {}".format(xml_name, xsl_path)):
        fixture_name = _read_file("{}{}.xml".format(conf.FIXTURES_ROOT, xml_name))
        file_data = fop.render(
            fixture_name, xsl_path, output_format, conf="tests/conf/fop.xconf"
        )

        return file_data


def _check_fail(render_call, allowed_list, expected_exception):
    fixture_name = render_call.args[0]

    if fixture_name in allowed_list:
        assert_that(render_call, raises(expected_exception))

        return True

    return False


@pytest.mark.parametrize("fixture_name", conf.INVOICE["all"])
def test_fop_rtf_invoice(fixture_name):
    with allure.step("test invoice RTF rendering"):
        render_call = calling(_render).with_args(
            fixture_name, XslPath.DEFAULT, fop.OutputFormat.RTF
        )
        fail = conf.INVOICE["fail"]

        (
            _check_fail(render_call, fail["rtf"], fop.FopException)
            or _check_fail(
                render_call,
                fail["template_not_found"],
                fop.TemplateNotFoundFopException,
            )
            or assert_that(render_call())
        )


@pytest.mark.parametrize("fixture_name", conf.ACT["all"])
def test_fop_rtf_act(fixture_name):
    with allure.step("test act RTF rendering"):
        assert_that(_render(fixture_name, XslPath.DEFAULT, fop.OutputFormat.RTF))


@pytest.mark.parametrize("fixture_name", conf.INVOICE["all"])
def test_fop_pdf_invoice(fixture_name):
    with allure.step("test invoice PDF rendering"):
        render_call = calling(_render).with_args(
            fixture_name, XslPath.DEFAULT, fop.OutputFormat.PDF
        )
        fail = conf.INVOICE["fail"]

        (
            _check_fail(
                render_call,
                fail["template_not_found"],
                fop.TemplateNotFoundFopException,
            )
            or assert_that(render_call())
        )


@pytest.mark.parametrize("fixture_name", conf.ACT["all"])
def test_fop_pdf_act(fixture_name):
    with allure.step("test act PDF rendering"):
        assert_that(_render(fixture_name, XslPath.DEFAULT, fop.OutputFormat.PDF))


@pytest.mark.parametrize("fixture_name", conf.CONTRACT["all"])
def test_fop_foout_contract(fixture_name):
    with allure.step("test contract FOOUT rendering"):
        assert_that(_render(fixture_name, XslPath.RIT, fop.OutputFormat.FO_OUT))


@pytest.mark.parametrize("fixture_name", ["invoice/invoice.sw_ur.usd.14"])
def test_fop_foout_invoice(fixture_name):
    with allure.step("test invoice FOOUT content"):
        expected_content_text = _read_file("./tests/expect/{}.xml".format(fixture_name))
        rendered_content_text = _render(
            fixture_name, XslPath.DEFAULT, fop.OutputFormat.FO_OUT
        )
        diff_text = _get_diff(expected_content_text, rendered_content_text)

        assert_that(diff_text, equal_to(""))
