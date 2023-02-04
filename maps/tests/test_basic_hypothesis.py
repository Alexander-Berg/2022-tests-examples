from yandex.maps.proto.bizdir.common.business_pb2 import (
    CompanyName,
    CompanyNames,
    CompanyState,
)
from yandex.maps.proto.bizdir.sps.hypothesis_pb2 import (
    CompanyNamesChange,
    CompanyNamesEvent,
    CompanyNamesEvents,
    CompanyStateChange,
    CompanyStateEvent,
    CompanyStateEvents,
    EditCompanyHypothesis,
    EventMetadata,
    EventSource,
    Hypothesis,
    HypothesisCompanyNames,
    HypothesisCompanyState,
)
from yandex.maps.proto.bizdir.sps.verdict_pb2 import (
    AttributeVerdict,
    CompanyNamesVerdict,
    CompanyStateVerdict,
    HypothesisVerdict,
    Verdict,
)

from maps.doc.proto.testhelper.validator import Validator

validator = Validator('bizdir/sps')


def test_basic_hypothesis():
    # Session with basic accept/reject verdicts
    message = Hypothesis(
        hypothesis_id="2f22a7410654a6129956273060f034b4",
        edit_company=EditCompanyHypothesis(
            company_id="1790421485",

            names=HypothesisCompanyNames(
                value=CompanyNames(lang_to_name={
                    'ru': CompanyName(name="Решелье")
                }),

                change_history=[
                    CompanyNamesChange(
                        events=CompanyNamesEvents(event=[
                            CompanyNamesEvent(
                                new_value=CompanyNames(lang_to_name={
                                    'ru': CompanyName(name="Решелье")
                                }),
                                metadata=EventMetadata(
                                    source=EventSource.SPS,
                                    timestamp=1622585306,
                                    imported=EventMetadata.Import(),
                                    message="Импорт атрибута из Справочника",
                                )
                            )
                        ])
                    )
                ],

                hypothesis=CompanyNamesEvents(event=[
                    CompanyNamesEvent(
                        new_value=CompanyNames(lang_to_name={
                            'ru': CompanyName(name="Ришелье")
                        }),
                        metadata=EventMetadata(
                            source=EventSource.SIGNAL,
                            timestamp=1622586342,
                            imported=EventMetadata.Import(
                                signal_id="sps1://feedback?id=fd0c26cf1950fc0f0108b50e60bdd212"
                            ),
                            message="Компания называется не так и вообще закрыта",
                        )
                    ),
                    CompanyNamesEvent(
                        metadata=EventMetadata(
                            source=EventSource.SPS,
                            timestamp=1622586442,
                            verdict_required=EventMetadata.RequireVerdict()
                        )
                    )
                ])
            ),

            company_state=HypothesisCompanyState(
                value=CompanyState.OPEN,

                change_history=[
                    CompanyStateChange(
                        events=CompanyStateEvents(event=[
                            CompanyStateEvent(
                                new_value=CompanyState.OPEN,
                                metadata=EventMetadata(
                                    source=EventSource.SPS,
                                    timestamp=1622585306,
                                    imported=EventMetadata.Import(),
                                    message="Импорт атрибута из Справочника",
                                )
                            )
                        ])
                    )
                ],

                hypothesis=CompanyStateEvents(event=[
                    CompanyStateEvent(
                        new_value=CompanyState.CLOSED,
                        metadata=EventMetadata(
                            source=EventSource.SIGNAL,
                            timestamp=1622586342,
                            imported=EventMetadata.Import(
                                signal_id="sps1://feedback?id=fd0c26cf1950fc0f0108b50e60bdd212"
                            ),
                            message="Компания называется не так и вообще закрыта",
                        )
                    ),
                    CompanyStateEvent(
                        metadata=EventMetadata(
                            source=EventSource.SPS,
                            timestamp=1622586442,
                            verdict_required=EventMetadata.RequireVerdict()
                        )
                    )
                ])
            )
        )
    )

    validator.validate_example(message, 'basic_hypothesis_0.prototxt')


def test_basic_verdict():
    message = Verdict(
        on_edit=HypothesisVerdict(
            names=CompanyNamesVerdict(
                verdict=AttributeVerdict(
                    accept=AttributeVerdict.Accept(),
                    message="В названии, действительно, опечатка",
                ),
                value=CompanyNames(lang_to_name={
                    'ru': CompanyName(name="Ришелье")
                })
            ),
            company_state=CompanyStateVerdict(
                verdict=AttributeVerdict(
                    reject=AttributeVerdict.Reject(),
                    message="На вчерашних(!) Зеркалах видно, что работает",
                ),
            )
        )
    )

    validator.validate_example(message, 'basic_verdict_0.prototxt')
