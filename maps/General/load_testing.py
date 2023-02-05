import logging
import re
import typing as tp

from pydantic import NonNegativeInt, constr, root_validator, validator
import yaml

from maps.infra.sedem.lib.config.schema.base import BaseModel
from maps.infra.sedem.lib.pydantic_oneof_validator import make_oneof_validator


logger = logging.getLogger(__name__)


FunctionLikeConfigItem = constr(regex=r'^[\w]+\(([\w%]+, ?)+[\w%]+\)$', max_length=100)
TestName = Slug = constr(regex=r'^[A-Za-z0-9-_/ ]+$', max_length=200)
SandboxTaskName = constr(regex=r'^[A-Z\d_]+$', max_length=200)
STARTREK_TICKET_REGEX = re.compile(r'^([A-Z]+)-(\d+)$')
StartrekTicket = constr(regex=STARTREK_TICKET_REGEX.pattern, max_length=50)


def get_st_queue(st_ticket: StartrekTicket) -> str:
    return STARTREK_TICKET_REGEX.fullmatch(st_ticket).group(1)


def phantom_to_pandora_load_profile(schedule: str) -> list[dict[str, tp.Union[int, str]]]:
    """
    The definition of this function is copied from
    https://a.yandex-team.ru/arc/trunk/arcadia/load/projects/tankapi_server/tankapi/config_tweaker.py?rev=r9023362#L21

    :param schedule: phantom load profile
    :return: pandora load profile
    """

    # fmt: off

    profile = []
    schedules = re.split(r'\) ', schedule.strip())
    for schedule in schedules:
        res = re.split(r'\(|\)|\,', schedule)

        if res[0].strip() == 'const':
            profile.append({
                'type': 'const',
                'ops': int(res[1].strip()),
                'duration': res[2].strip()
            })
        elif res[0].strip() == 'line':
            profile.append({
                'type': 'line',
                'from': int(res[1].strip()),
                'to': int(res[2].strip()),
                'duration': res[3].strip()
            })
        elif res[0].strip() == 'step':
            profile.append({
                'type': 'step',
                'from': int(res[1].strip()),
                'to': int(res[2].strip()),
                'step': int(res[3].strip()),
                'duration': res[4].strip()
            })

    return profile

    # fmt: on


class Ammo(BaseModel):
    type: tp.Literal['access', 'phantom', 'raw', 'uri', 'uripost']
    url: str


class PandoraConfig(BaseModel):
    protocol: tp.Literal['http', 'https', 'http2'] = 'http'


class PhantomConfig(BaseModel):
    protocol: tp.Literal['http', 'https'] = 'http'
    http_version: tp.Literal['1.0', '1.1'] = '1.1'

    @validator('http_version', pre=True)
    def convert_float_http_version_to_str(cls, v: tp.Any) -> tp.Any:
        if isinstance(v, float):
            return str(v)
        return v


class GenericLoadConfig(BaseModel):
    load_profiles: list[FunctionLikeConfigItem]
    autostop: list[FunctionLikeConfigItem]
    ammo: Ammo
    instances: NonNegativeInt = 1000
    tank_type: tp.Literal['sandbox', 'rtc'] = 'sandbox'
    pandora: tp.Optional[PandoraConfig] = None
    phantom: tp.Optional[PhantomConfig] = None

    # generated in make_generated_fields
    protocol: str = None
    encryption: str = None
    port: int = None
    formatted_load_profiles: str = None
    formatted_tank_type: str = None

    # validators
    @root_validator(pre=True)
    def validate_has_one_load_generator(cls, values: dict[str, tp.Any]) -> dict[str, tp.Any]:
        return make_oneof_validator('pandora', 'phantom')(values)

    @root_validator
    def make_generated_fields(cls, values: dict[str, tp.Any]) -> dict[str, tp.Any]:
        values['protocol'] = cls._calculate_protocol(pandora=values.get('pandora'), phantom=values.get('phantom'))
        values['encryption'] = cls._calculate_encryption(values['protocol'])
        values['port'] = 443 if values['encryption'] else 80
        values['formatted_tank_type'] = (
            'nanny:maps_core_tanks_load' if values.get('tank_type') == 'rtc' else values.get('tank_type')
        )
        # load_profiles existence will be validated in the type annotation after root_validator
        if (load_profiles := values.get('load_profiles')) is not None:
            values['formatted_load_profiles'] = cls._format_load_profiles(
                load_profiles=load_profiles,
                has_pandora=values.get('pandora') is not None,
                has_phantom=values.get('phantom') is not None,
            )
        return values

    @classmethod
    def _calculate_protocol(cls, *, pandora: tp.Optional[PandoraConfig], phantom: tp.Optional[PhantomConfig]) -> str:
        if pandora is not None:
            return pandora.protocol
        elif phantom is not None:
            return phantom.protocol
        else:
            assert False, 'Existence of pandora or phantom should have been validated before'

    @classmethod
    def _calculate_encryption(cls, protocol: str) -> int:
        if protocol == 'http':
            return False
        elif protocol in ('https', 'http2'):
            return True
        else:
            raise NotImplementedError(f'Calculating encryption is not implemented for {protocol = }')

    @classmethod
    def _format_load_profiles(
        cls,
        *,
        load_profiles: list[FunctionLikeConfigItem],
        has_pandora: bool,
        has_phantom: bool,
    ) -> tp.Optional[str]:

        phantom_load_profiles = ' '.join(load_profiles)

        if has_phantom and not has_pandora:
            return phantom_load_profiles

        elif has_pandora and not has_phantom:

            profiles = phantom_to_pandora_load_profile(phantom_load_profiles)

            # default_flow_style=True is to force load_profiles to be in the format of [{...}, {...}]
            # This ensures that the value does not break the yaml rendered from the jinja.
            # Otherwise there would be a question: how much indent do I need to add to load_profiles sub-yaml?
            return yaml.safe_dump(profiles, default_flow_style=True)

        else:
            assert False, 'Existence of pandora xor phantom should have been validated before'


class CustomLoadConfig(BaseModel):
    task_template: SandboxTaskName


class LoadTestSpec(BaseModel):
    slug: tp.Optional[Slug] = None
    st_ticket: tp.Optional[StartrekTicket] = None
    sla_rps: NonNegativeInt = 0

    generic_load_config: tp.Optional[GenericLoadConfig] = None
    custom_load_config: tp.Optional[CustomLoadConfig] = None

    # validators
    @root_validator(skip_on_failure=True)
    def validate_generic_or_custom(cls, values: dict[str, tp.Any]) -> dict[str, tp.Any]:
        return make_oneof_validator('generic_load_config', 'custom_load_config')(values)


def get_default_slug(*, sedem_service_name: str, test_name: str):
    return f'{sedem_service_name} {test_name}'


class LoadTestingSection(BaseModel):
    st_ticket: StartrekTicket
    tests: dict[TestName, LoadTestSpec]
