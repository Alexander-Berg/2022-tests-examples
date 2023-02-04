"""
ConfNode classes validation tests
* class validation called from reconf.ConfNode.__init_subclass__()

"""
import pytest

from infra.reconf import ConfNode, ValidationError


# doc_url attr presence tests
class DocUrlValidationBaseClass(ConfNode):
    doc_url = 'https://example.com/path/to/README.md'
    validate_class_doc_url = True  # disabled by default
    validate_class_docstring = False  # enabled by default


def test_doc_url_absent():
    with pytest.raises(ValidationError):
        class UrllessClass(DocUrlValidationBaseClass):
            pass


def test_doc_url_present():
    class UrllessClass(DocUrlValidationBaseClass):
        doc_url = 'exists'


def test_validate_doc_url_method_overrided():
    class UrllessClass(DocUrlValidationBaseClass):
        @classmethod
        def validate_doc_url(cls):
            pass


# docstring presence tests
class DocstringValidationBaseClass(ConfNode):
    """ docstring """


def test_docstring_absent():
    with pytest.raises(ValidationError):
        class DoclessClass(DocstringValidationBaseClass):
            pass


def test_docstring_present():
    class DoclessClass(DocstringValidationBaseClass):
        """ docstring """


def test_validate_docstring_method_overrided():
    class DoclessClass(DocstringValidationBaseClass):
        @classmethod
        def validate_docstring(cls):
            pass


# global validation tests
def test_class_validation_disabled():
    class DoclessClass(ConfNode):
        validate_class = False


def test_validate_method_overrided():
    class DoclessClass(ConfNode):
        @classmethod
        def validate(cls):
            pass
