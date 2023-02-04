from unittest import TestCase, mock

from common.u import U


class TestU(TestCase):

    @mock.patch('common.U.logging.getLogger')
    def test_suppress_positive(self, mlogging):
        logger_mock = mock.MagicMock()
        mlogging.return_value = logger_mock
        expected = True
        raiser = mock.MagicMock(side_effect=Exception)
        raiser.__name__ = 'mock'
        raiser.__annotations__ = {}
        U.suppress(verbose=expected)(raiser)()
        err_call = logger_mock.method_calls[0]
        *_, a, kw = err_call
        self.assertEqual(kw['exc_info'], expected)
        self.assertEqual(1, len(logger_mock.method_calls))

    @mock.patch('common.U.logging.getLogger')
    def test_suppress_desired(self, mlogging):
        expected = TypeError
        logger_mock = mock.MagicMock()
        mlogging.return_value = logger_mock

        raiser = mock.MagicMock(side_effect=expected())
        raiser.__name__ = 'mock'
        raiser.__annotations__ = {}
        U.suppress(expected)(raiser)()
        self.assertEqual(1, raiser.call_count)
        self.assertFalse(logger_mock.called)

        expect_thrown = Exception
        raiser.reset_mock()
        raiser.side_effect = expect_thrown()
        self.assertRaises(expect_thrown, U.suppress(expected)(raiser), expect_thrown)
        self.assertEqual(1, raiser.call_count)

    @mock.patch('common.U.logging.getLogger')
    def test_suppres_can_wrap_function_wo_params(self, mlogging):
        expected = TypeError
        logger_mock = mock.MagicMock()
        mlogging.return_value = logger_mock
        raiser = mock.MagicMock(side_effect=expected())
        raiser.__name__ = 'mock'
        raiser.__annotations__ = {}
        U.suppress(raiser)()
        self.assertEqual(1, raiser.call_count)
