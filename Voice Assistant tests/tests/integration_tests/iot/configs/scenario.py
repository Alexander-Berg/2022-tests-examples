from alice.megamind.protos.common.iot_pb2 import TIoTUserInfo
from alice.protos.data.device.info_pb2 import EUserDeviceType
from alice.protos.data.location.room_pb2 import TUserRoom

TCapability = TIoTUserInfo.TCapability
TProperty = TIoTUserInfo.TProperty
ECapabilityType = TCapability.ECapabilityType

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

scenario = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[
        TIoTUserInfo.TScenario(
            Id='evo-test-scenario-id-1',
            Name='Выключи звук',
            Triggers=[
                TIoTUserInfo.TScenario.TTrigger(
                    Type=TIoTUserInfo.TScenario.TTrigger.VoiceScenarioTriggerType,
                    VoiceTriggerPhrase='Выключи звук',
                ),
            ],
            IsActive=True,
            Steps=[
                TIoTUserInfo.TScenario.TStep(
                    Type=TIoTUserInfo.TScenario.TStep.ActionsScenarioStepType,
                    ScenarioStepActionsParameters=TIoTUserInfo.TScenario.TStep.TScenarioStepActionsParameters(
                        Devices=[
                            TIoTUserInfo.TScenario.TLaunchDevice(
                                Id='evo-test-lamp-id-1',
                                Name='Музыка',
                                Type=EUserDeviceType.LightDeviceType,
                                Capabilities=scenario_lamp_capabilities,
                            ),
                        ],
                    ),
                ),
            ],
        )
    ],
    Colors=[
        TIoTUserInfo.TColor(
            Id='cold_white',
            Name='Холодный белый',
        ),
    ],
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
            Name='Музыка',
            Aliases=[],
            SkillId='QUALITY',
            Status=TIoTUserInfo.TDevice.EDeviceState.OnlineDeviceState,
            Type=EUserDeviceType.LightDeviceType,
            OriginalType=EUserDeviceType.LightDeviceType,
            AnalyticsType='devices.types.light',
            RoomId='room-test-id-1',
            Capabilities=scenario_lamp_capabilities,
        ),
    ]
)

station_scenario = TIoTUserInfo(
    RawUserInfo='{}',
    Scenarios=[
        TIoTUserInfo.TScenario(
            Id='evo-test-scenario-id-1',
            Name='Елочка гори',
            Triggers=[
                TIoTUserInfo.TScenario.TTrigger(
                    Type=TIoTUserInfo.TScenario.TTrigger.VoiceScenarioTriggerType,
                    VoiceTriggerPhrase='Елочка гори',
                ),
            ],
            IsActive=True,
            Steps=[
                TIoTUserInfo.TScenario.TStep(
                    Type=TIoTUserInfo.TScenario.TStep.ActionsScenarioStepType,
                    ScenarioStepActionsParameters=TIoTUserInfo.TScenario.TStep.TScenarioStepActionsParameters(
                        Devices=[
                            TIoTUserInfo.TScenario.TLaunchDevice(
                                Id='evo-test-lamp-id-1',
                                Name='Лампочка',
                                Type=EUserDeviceType.LightDeviceType,
                                Capabilities=[
                                    TCapability(
                                        Type=ECapabilityType.OnOffCapabilityType,
                                        OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                                        OnOffCapabilityState=TCapability.TOnOffCapabilityState(
                                            Value=True,
                                        ),
                                        AnalyticsType='devices.capabilities.on_off',
                                        Retrievable=True,
                                        Reportable=True,
                                    ),
                                ],
                            ),
                        ],
                    ),
                ),
            ],
        )
    ],
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
            Capabilities=[
                TCapability(
                    Type=ECapabilityType.OnOffCapabilityType,
                    OnOffCapabilityParameters=TCapability.TOnOffCapabilityParameters(),
                    OnOffCapabilityState=TCapability.TOnOffCapabilityState(
                        Value=False,
                    ),
                    AnalyticsType='devices.capabilities.on_off',
                    Retrievable=True,
                    Reportable=True,
                ),
            ],
        ),
    ],
)
