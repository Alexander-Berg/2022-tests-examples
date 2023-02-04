from alice.megamind.protos.common.iot_pb2 import TIoTUserInfo
from alice.protos.data.device.info_pb2 import EUserDeviceType
from alice.protos.data.location.room_pb2 import TUserRoom

TCapability = TIoTUserInfo.TCapability
TProperty = TIoTUserInfo.TProperty
ECapabilityType = TCapability.ECapabilityType

double_names = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Зал',
            HouseholdId='evo-test-household-id-1',
        ),
    ],
    Households=[
        TIoTUserInfo.THousehold(
            Id='evo-test-household-id-1',
            Name='Дом',
        ),
    ],
    CurrentHouseholdId='evo-test-household-id-1',
    Groups=[],
    Devices=[
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            GroupIds=[],
            Name='Моя лампочка',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=[
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
            ]
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-2',
            ExternalId='evo-test-lamp-external-id-2',
            GroupIds=[],
            Name='Лампочка 1',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=[
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

lamp_turn_off_change_brightness = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Зал',
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
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            Name='Лампочка 1',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-2',
            ExternalId='evo-test-lamp-external-id-2',
            Name='Лампочка 2',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)

lamp_turn_on_all = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[],
    Colors=[],
    Rooms=[
        TUserRoom(
            Id='room-test-id-1',
            Name='Зал',
            HouseholdId='evo-test-household-id-1',
        ),
        TUserRoom(
            Id='room-test-id-2',
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
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            Name='Лампочка 1',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-2',
            ExternalId='evo-test-lamp-external-id-2',
            Name='Лампочка 2',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-3',
            ExternalId='evo-test-lamp-external-id-3',
            Name='Лампочка 3',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-2',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)

lamp_similar_names_all_lamps = TIoTUserInfo(
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
            Id='evo-test-lamp-id-1',
            ExternalId='evo-test-lamp-external-id-1',
            Name='Лампочка',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-2',
            ExternalId='evo-test-lamp-external-id-2',
            Name='Лампочка',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-3',
            ExternalId='evo-test-lamp-external-id-3',
            Name='Лампочка пять',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-4',
            ExternalId='evo-test-lamp-external-id-4',
            Name='Лампочка семь',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)

lamp_similar_names_two_lamps = TIoTUserInfo(
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
            Id='evo-test-lamp-id-3',
            ExternalId='evo-test-lamp-external-id-3',
            Name='Лампочка пять',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
        TIoTUserInfo.TDevice(
            HouseholdId='evo-test-household-id-1',
            Id='evo-test-lamp-id-4',
            ExternalId='evo-test-lamp-external-id-4',
            Name='Лампочка семь',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)

lamp_similar_names_one_lamp = TIoTUserInfo(
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
            Id='evo-test-lamp-id-4',
            ExternalId='evo-test-lamp-external-id-4',
            Name='Лампочка семь',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
    ],
)
