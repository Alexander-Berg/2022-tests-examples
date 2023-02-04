import abc
import typing as tp

from dataclasses import dataclass

from billing.hot.tests.lib.rand import rand
from billing.hot.tests.lib.util import util


@dataclass
class Contract(abc.ABC):
    id: int
    _service_ids: tuple = ()

    @classmethod
    def name(cls) -> str:
        return cls.__name__

    @classmethod
    def generate(cls) -> 'Contract':
        return cls(id=rand.int32())

    @classmethod
    def params(cls, extended_params: tp.Optional[dict] = None) -> dict:
        return util.merge({
            'collaterals': {
                '0': {
                    'services': {
                        str(service_id): 1
                        for service_id in cls._service_ids
                    }
                }
            }
        }, extended_params or {})

    def add_to(self, dct: dict[str, 'Contract']) -> None:
        dct[self.name()] = self


class ServiceContract(Contract):
    _service_ids = 124, 128


class SubventionContract(Contract):
    _service_ids = 137,


class TransferContract(Contract):
    _service_ids = 222,


class CorporateContract(Contract):
    _service_ids = 651,


class LogisticsContract(Contract):
    _service_ids = 719,


class OplataContract(Contract):
    _service_ids = 625,


class BnplContract(Contract):
    _service_ids = 1125,


class BnplIncomeContract(Contract):
    _service_ids = 1190,


class MailProContract(Contract):
    _service_ids = 638, 118


class DiskContract(Contract):
    _service_ids = 116,


class GamesContract(Contract):
    _service_ids = 658,


class AeroContract(Contract):
    _service_ids = 607,


class MusicContract(Contract):
    _service_ids = 23, 711


class MusicMediaservicesContract(Contract):
    _service_ids = 685, 714
