# coding: utf-8

class AbstractProcessor(object):
    def generate_rows(self, data):
        raise NotImplementedError('generate_rows not implemented')

    def compare(self):
        raise NotImplementedError('compare not implemented')


class AbstractTestsProvider(object):
    def provide_tests(self):
        raise NotImplementedError('provide_tests not implemented')


class BaseTestData(object):
    def __init__(self, partner_integration_params, context, test_input, test_output, ctype):
        self.partner_integration_params = partner_integration_params
        self.context = context
        self.test_input = test_input
        self.test_output = test_output
        self.ctype = ctype
