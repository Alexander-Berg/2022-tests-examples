# -*- coding: utf-8 -*-
class SpecialValue(object):
    @staticmethod
    def get_value(ctx):
        raise NotImplementedError()


class SSL(object):
    class CAFile(SpecialValue):
        @staticmethod
        def get_value(ctx):
            return ctx.certs.abs_path('root_ca.crt')

    class Key(SpecialValue):
        @staticmethod
        def get_value(ctx):
            return ctx.certs.abs_path('default.key')

    class Cert(SpecialValue):
        @staticmethod
        def get_value(ctx):
            return ctx.certs.abs_path('default.crt')

    class Ticket(SpecialValue):
        @staticmethod
        def get_value(ctx):
            return ctx.certs.abs_path('default_ticket.0.raw')

    class OCSP(SpecialValue):
        @staticmethod
        def get_value(ctx):
            return ctx.certs.abs_path('default_ocsp.0.der')


class LaasTimeoutOneSecond(SpecialValue):
    @staticmethod
    def get_value(ctx):
        return '1s'


class TwoWorkers(SpecialValue):
    @staticmethod
    def get_value(ctx):
        return 2


class WeightsDir(SpecialValue):
    @staticmethod
    def get_value(ctx):
        return ctx.manager.fs.root_dir


class LogsDir(SpecialValue):
    @staticmethod
    def get_value(ctx):
        return ctx.manager.fs.root_dir


class ClickDaemonKeys(SpecialValue):
    @staticmethod
    def get_value(ctx):
        return ctx.certs.abs_path('clickdaemon.keys')
