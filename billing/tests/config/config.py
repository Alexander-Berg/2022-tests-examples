import os
import typing as tp

import yaml
from dataclasses import dataclass


@dataclass
class BaseConfig:
    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]):
        if not dct:
            return None
        params = {}
        for key, field in cls.__dataclass_fields__.items():
            val = dct.get(key)
            if callable(getattr(field.type, 'from_dict', None)):
                val = field.type.from_dict(val or {})
            params[key] = val
        return cls(**params)


@dataclass
class YavSecret(BaseConfig):
    id: str
    key: str


@dataclass
class TvmConfig(BaseConfig):
    src_id: int
    secret: YavSecret


@dataclass
class YtConfig(BaseConfig):
    proxy: str
    tables: dict[str, str]  # name: path


@dataclass
class DatabaseConfig(BaseConfig):
    dsn: str
    db_name: str
    user: str
    host: str
    port: int
    password_secret: tp.Optional[YavSecret]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> tp.Optional['DatabaseConfig']:
        dsn = dct.get('dsn')
        if dsn:
            dct['dsn'] = os.getenv(dsn.get('env')) or dsn.get('default')

        return super().from_dict(dct)


@dataclass
class ShardedDatabaseConfig:
    shards: list[DatabaseConfig]

    @staticmethod
    def from_dict(dct: dict[str, tp.Any]) -> tp.Optional['ShardedDatabaseConfig']:
        if not dct:
            return None
        return ShardedDatabaseConfig(shards=[DatabaseConfig.from_dict(shard) for shard in dct['shards']])


@dataclass
class LBConsumerConfig(BaseConfig):
    reader: str
    topic: str


@dataclass
class LBProducerConfig:
    topic: str
    source_id: bytes

    @staticmethod
    def from_dict(dct: tp.Optional[dict[str, tp.Any]]) -> tp.Optional['LBProducerConfig']:
        if not dct:
            return None
        return LBProducerConfig(topic=dct['topic'], source_id=bytes(dct['source_id'], encoding='ascii'))


@dataclass
class LBConfig:
    endpoint: str
    port: int
    tvm_id: tp.Optional[int]

    consumers: dict[str, LBConsumerConfig]
    producers: dict[str, LBProducerConfig]

    @staticmethod
    def from_dict(dct: dict[str, tp.Any]) -> tp.Optional['LBConfig']:
        dct['endpoint'] = os.getenv(dct['endpoint'].get('env', '')) or dct['endpoint'].get('default')
        dct['port'] = os.getenv(dct['port'].get('env', '')) or dct['port'].get('default')
        if not dct:
            return None
        return LBConfig(
            endpoint=dct['endpoint'],
            port=dct['port'],
            consumers={name: LBConsumerConfig.from_dict(consumer)
                       for name, consumer in dct.get('consumers', {}).items()},
            producers={name: LBProducerConfig.from_dict(producer)
                       for name, producer in dct.get('producers', {}).items()},
            tvm_id=dct.get('tvm_id'),
        )


@dataclass
class OEBSConfig(BaseConfig):
    template_dir: str
    log_broker: dict[str, LBConfig]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]):
        if not dct:
            return None
        return cls(
            template_dir=dct.get('template_dir', None),
            log_broker={namespace: LBConfig.from_dict(cfg)
                        for namespace, cfg in dct.get('log_broker', {}).items()}
        )


@dataclass
class AccrualerConfig(BaseConfig):
    log_broker: LBConfig


@dataclass
class RestClientConfig(BaseConfig):
    url: str
    tvm_id: tp.Optional[int]
    handlers: BaseConfig

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]):
        dct['url'] = os.getenv(dct['url']['env']) or dct['url']['default']
        return super().from_dict(dct)


@dataclass
class PayoutHandlersConfig(BaseConfig):
    payout_by_client: str
    payout_info: str


@dataclass
class PayoutConfig(RestClientConfig):
    handlers: PayoutHandlersConfig
    db: DatabaseConfig


@dataclass
class ProcessorHandlersConfig(BaseConfig):
    process: str


@dataclass
class ProcessorConfig(RestClientConfig):
    handlers: ProcessorHandlersConfig
    template_dir: str


@dataclass
class AccountsHandlersConfig(BaseConfig):
    read_batch: str
    write_batch: str
    turnover_detailed: str
    exported_events: str


@dataclass
class AccountsConfig(RestClientConfig):
    handlers: AccountsHandlersConfig
    template_dir: str


@dataclass
class StateBuilderConfig(BaseConfig):
    yt_data_dir: str


@dataclass
class TestConfigurationConfig(BaseConfig):
    xlsx_data_dir: str


@dataclass
class DiodHandlerConfig(BaseConfig):
    batch: str


@dataclass
class DiodConfig(BaseConfig):
    url: str
    tvm_id: int
    service_id: str
    handlers: DiodHandlerConfig


@dataclass
class Config:
    accounts: tp.Optional[AccountsConfig]
    accounts_db: tp.Optional[ShardedDatabaseConfig]
    accrualer: tp.Optional[AccrualerConfig]
    oebs: tp.Optional[OEBSConfig]
    payout: tp.Optional[PayoutConfig]
    processor: tp.Optional[ProcessorConfig]
    state_builder: tp.Optional[StateBuilderConfig]
    test_configuration: tp.Optional[TestConfigurationConfig]
    tvm: tp.Optional[TvmConfig]
    yt: tp.Optional[YtConfig]
    diod: tp.Optional[DiodConfig]
    testing_namespaces: dict[str, list[str]]  # <test_type>: [<namespace>]

    @staticmethod
    def from_dict(dct: dict[str, tp.Any]) -> 'Config':
        return Config(
            accounts=AccountsConfig.from_dict(dct.get('accounts')),
            accounts_db=ShardedDatabaseConfig.from_dict(dct.get('accounts_db')),
            accrualer=AccrualerConfig.from_dict(dct.get('accrualer')),
            oebs=OEBSConfig.from_dict(dct.get('oebs')),
            payout=PayoutConfig.from_dict(dct.get('payout')),
            processor=ProcessorConfig.from_dict(dct.get('processor')),
            state_builder=StateBuilderConfig.from_dict(dct.get('state_builder')),
            test_configuration=TestConfigurationConfig.from_dict(dct.get('test_configuration')),
            tvm=TvmConfig.from_dict(dct.get('tvm')),
            yt=YtConfig.from_dict(dct.get('yt')),
            diod=DiodConfig.from_dict(dct.get('diod')),
            testing_namespaces=dct.get('testing_namespaces')
        )

    @staticmethod
    def from_yaml(yaml_data: bytes) -> 'Config':
        yaml_dict = yaml.safe_load(yaml_data)
        return Config.from_dict(yaml_dict)


@dataclass
class YtTable(BaseConfig):
    path: str
    schema: list[dict[str, tp.Any]]
    atomicity: str


@dataclass
class YtTables:
    personal_accounts: YtTable
    personal_account_client_idx: YtTable
    contract_client_idx: YtTable
    page_data: YtTable
    migration_info: YtTable
    firm_tax: YtTable
    partner_products: YtTable
    overdraft_service_limits: YtTable
    overdraft_client_limits: YtTable
    contracts: YtTable
    accruals_common_dry_run: YtTable
    iso_currency_rate: YtTable

    @property
    def tables(self) -> list[YtTable]:
        return [getattr(self, table) for table in getattr(self, '__dataclass_fields__')]

    @property
    def defined_tables(self) -> list[YtTable]:
        return [table for table in self.tables if table.schema != []]

    @classmethod
    def from_dict(cls, dct: dict[str, tp.Any]) -> 'YtTables':
        tables = {
            table_name: YtTable.from_dict(dct.get(table_name))
            for table_name in getattr(cls, '__dataclass_fields__')
        }
        return YtTables(**tables)

    @classmethod
    def from_yaml(cls, yaml_data: bytes) -> 'YtTables':
        yaml_dict = yaml.safe_load(yaml_data)
        return cls.from_dict(yaml_dict)
