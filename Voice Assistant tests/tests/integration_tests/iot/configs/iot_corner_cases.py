from alice.megamind.protos.common.iot_pb2 import TIoTUserInfo
from alice.protos.data.device.info_pb2 import EUserDeviceType
from alice.protos.data.location.room_pb2 import TUserRoom

TCapability = TIoTUserInfo.TCapability
TProperty = TIoTUserInfo.TProperty
ECapabilityType = TCapability.ECapabilityType

multiple_devices = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Дом',
        ),
    ],
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            Id='evo-test-vacuum-id-1',
            ExternalId='evo-test-vacuum-external-id-1',
            Name='Аркадий',
            Aliases=[],
            SkillId='QUALITY',
            Type=EUserDeviceType.VacuumCleanerDeviceType,
            OriginalType=EUserDeviceType.VacuumCleanerDeviceType,
            AnalyticsType='devices.types.vacuum_cleaner',
            HouseholdId='evo-test-household-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(
                        Split=True
                    ),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(
                        Instance='on',
                        Value=False,
                    ),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
                TCapability(
                    Type=ECapabilityType.ToggleCapabilityType,
                    ToggleCapabilityParameters=TCapability.TToggleCapabilityParameters(),
                    ToggleCapabilityState=TCapability.TToggleCapabilityState(
                        Instance='pause',
                        Value=False,
                    ),
                    AnalyticsType='devices.capabilities.toggle',
                    Retrievable=True,
                    Reportable=True,
                )
            ]
        ),
        TIoTUserInfo.TDevice(
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            Name='Лампочка',
            Aliases=[],
            SkillId='QUALITY',
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            HouseholdId='evo-test-household-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ]
)

scenario_lamp_capabilities = [
    TCapability(
        Type=ECapabilityType.OnOffCapabilityType,
        OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
        OnOffCapabilityState=TCapability.TOnOffCapabilityState(
            Instance='on',
            Value=True,
        ),
        AnalyticsType='devices.capabilities.on_off',
        Retrievable=True,
        Reportable=True,
    ),
    TCapability(
        Type=ECapabilityType.ColorSettingCapabilityType,
        ColorSettingCapabilityParameters=TCapability.TColorSettingCapabilityParameters(
            TemperatureK=TCapability.TColorSettingCapabilityParameters.TTemperatureKCapabilityParameters(
                Min=2000,
                Max=9000,
            ),
            ColorSceneParameters=TCapability.TColorSettingCapabilityParameters.TColorSceneParameters(),
        ),
        ColorSettingCapabilityState=TCapability.TColorSettingCapabilityState(
            Instance='temperature_k',
            TemperatureK=4000,
        ),
        AnalyticsType='devices.capabilities.color_setting',
        Retrievable=True,
        Reportable=False,
    ),
    TCapability(
        Type=ECapabilityType.RangeCapabilityType,
        RangeCapabilityParameters=TCapability.TRangeCapabilityParameters(
            Range=TCapability.TRangeCapabilityParameters.TRange(
                Min=1,
                Max=100,
                Precision=1,
            ),
            RandomAccess=True,
            Unit='unit.percent',
            Instance='brightness',
            Looped=False,
        ),
    ),
]

lamp_without_room = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            Name='Лампочка',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)

xiaomi_kettle = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-kettle-id-1',
            ExternalId='evo-test-kettle-external-id-1',
            Name='sky kettle',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.KettleDeviceType,
            OriginalType=EUserDeviceType.KettleDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_socket = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-socket-id-1',
            ExternalId='evo-test-socket-external-id-1',
            Name='sky socket',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.SocketDeviceType,
            OriginalType=EUserDeviceType.SocketDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_bulb = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-bulb-id-1',
            ExternalId='evo-test-bulb-external-id-1',
            Name='smart bulb',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_outlet = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-outlet-id-1',
            ExternalId='evo-test-outlet-external-id-1',
            Name='aqara wall outlet',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.SocketDeviceType,
            OriginalType=EUserDeviceType.SocketDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_vacuum = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-vacuum-id-1',
            ExternalId='evo-test-vacuum-external-id-1',
            Name='robot vacuum',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.VacuumCleanerDeviceType,
            OriginalType=EUserDeviceType.VacuumCleanerDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_strip = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-strip-id-1',
            ExternalId='evo-test-strip-external-id-1',
            Name='mi smart power strip',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_multibaker = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-multibaker-id-1',
            ExternalId='evo-test-multibaker-external-id-1',
            Name='multibaker',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.CookingDeviceType,
            OriginalType=EUserDeviceType.CookingDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_switch = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-switch-id-1',
            ExternalId='evo-test-switch-external-id-1',
            Name='switch 2',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.SwitchDeviceType,
            OriginalType=EUserDeviceType.SwitchDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)

xiaomi_coffeemaker = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Кухня',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Квартира',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-coffeemaker-id-1',
            ExternalId='evo-test-coffeemaker-external-id-1',
            Name='coffee maker',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.CoffeeMakerDeviceType,
            OriginalType=EUserDeviceType.CoffeeMakerDeviceType,
            RoomId='room-test-id-1',
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ]
        ),
    ],
)