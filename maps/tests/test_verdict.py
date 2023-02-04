import maps.bizdir.sps.workstation.api.pb as pb
import maps.bizdir.sps.db.tables as db
from maps.bizdir.sps.workstation.api.session_wrapper import SessionWrapper
from maps.bizdir.sps.workstation.api.verdict import (
    InvalidDataError,
    _add_verdict_event,
    _add_unsuccessful_validation,
    _apply_attribute_verdict,
    _apply_feature_verdict,
    _apply_verdicts,
    _build_business_diff,
    _is_attribute_in_state,
    _is_hypothesis_in_state,
    _is_validation_requested,
    _list_hypothesis_fields,
    _process_validation_requested,
    _to_event_metadata,
    _update_attribute_history,
    _update_features_history,
    _update_hypothesis,
    _update_org_history,
    finish_hypothesis,
    process_edit_verdict,
)

import pytest
from collections.abc import Generator
from typing import Any
from unittest import mock


PATH = "maps.bizdir.sps.workstation.api.verdict"


def test_add_verdict_event__for_rejected_verdict__adds_it() -> None:
    emails = pb.EmailsEvents()

    _add_verdict_event(emails, pb.AttributeReject(), "user_id")

    assert len(emails.event) == 1
    assert emails.event[0].metadata.HasField("source")
    assert emails.event[0].metadata.content_manager.login == "user_id"
    assert emails.event[0].metadata.HasField("rejected")


def test_add_verdict_event__for_refused_verdict__adds_it() -> None:
    emails = pb.EmailsEvents()

    _add_verdict_event(emails, pb.EventRefuse(), "user_id")

    assert len(emails.event) == 1
    assert emails.event[0].metadata.HasField("source")
    assert emails.event[0].metadata.content_manager.login == "user_id"
    assert emails.event[0].metadata.HasField("refused")


class Fixture:
    def __init__(
        self,
        _update_hypothesis: mock.Mock,
        _add_verdict_event: mock.Mock,
        task_creator: mock.Mock,
        logger: mock.Mock,
        _add_unsuccessful_validation_mock: mock.Mock,
    ) -> None:
        self.session = mock.Mock(autospec=SessionWrapper)
        self.hypothesis = db.Hypothesis(
            id="id",
            hypothesis=pb.Hypothesis(hypothesis_id="").SerializeToString(),
        )
        self.org_info = db.OrgInfo(id="permalink")
        self.session.org_info_for_update = lambda _: self.org_info

        self._update_hypothesis = _update_hypothesis
        self._add_verdict_event = _add_verdict_event
        self._add_unsuccessful_validation = _add_unsuccessful_validation_mock
        self.task_creator = task_creator.return_value
        self.logger = logger


@pytest.fixture
def f() -> Generator:
    with (
        mock.patch(f"{PATH}._update_hypothesis") as f1,
        mock.patch(f"{PATH}._add_verdict_event") as f2,
        mock.patch(f"{PATH}.TaskCreator") as f3,
        mock.patch(f"{PATH}._logger") as f4,
        mock.patch(f"{PATH}._add_unsuccessful_validation") as f5,
    ):
        yield Fixture(f1, f2, f3, f4, f5)


def test_finish_hypothesis__for_wrong_verdict__raises(f: Fixture) -> None:
    verdict = pb.Verdict(on_edit=pb.HypothesisVerdict())

    with pytest.raises(Exception):
        finish_hypothesis(f.session, f.hypothesis, verdict, "user_id")


def test_finish_hypothesis__for_refuse_verdict__refuses(f: Fixture) -> None:
    verdict = pb.Verdict(refuse=pb.RefuseVerdict())

    finish_hypothesis(f.session, f.hypothesis, verdict, "user_id")

    assert f.hypothesis.status == db.HypothesisStatus.refused
    f.session.update_signal_status.assert_called_once_with(
        f.hypothesis.id,
        pb.SignalResult(refused=pb.SignalRefuse()),
    )
    f._update_hypothesis.assert_called_once_with(
        mock.ANY, pb.EventRefuse(), "user_id"
    )


def test_finish_hypothesis__for_reject_verdict__rejects(f: Fixture) -> None:
    verdict = pb.Verdict(reject=pb.RejectVerdict())

    finish_hypothesis(f.session, f.hypothesis, verdict, "user_id")

    assert f.hypothesis.status == db.HypothesisStatus.rejected
    f.session.update_signal_status.assert_called_once_with(
        f.hypothesis.id,
        pb.SignalResult(rejected=pb.SignalReject()),
    )
    f._update_hypothesis.assert_called_once_with(
        mock.ANY, pb.AttributeReject(), "user_id"
    )


@mock.patch(f"{PATH}._update_org_history")
def test_finish_hypothesis__given_org_id__updates_org_history(
    _update_org_history: mock.Mock,
    f: Fixture,
) -> None:
    verdict = pb.Verdict(refuse=pb.RefuseVerdict())
    f.hypothesis.org_id = "permalink"
    f.session.org_info_for_update.return_value = db.OrgInfo(info=b"")

    finish_hypothesis(f.session, f.hypothesis, verdict, "user_id")

    _update_org_history.assert_called_once()


@mock.patch(f"{PATH}._list_hypothesis_fields")
def test_update_hypothesis__for_composite_field__adds_event(
    _list_hypothesis_fields: mock.Mock,
    f: Fixture,
) -> None:
    emails = pb.EmailsEvents()
    emails.event.add().new_value.email.add().value = "email"
    _list_hypothesis_fields.return_value = iter([("emails", emails)])

    _update_hypothesis(pb.Hypothesis(), pb.EventRefuse(), "user_id")

    f._add_verdict_event.assert_called_once_with(
        emails, pb.EventRefuse(), "user_id"
    )


@mock.patch(f"{PATH}._list_hypothesis_fields")
def test_update_hypothesis__for_repeated_field__adds_event(
    _list_hypothesis_fields: mock.Mock,
    f: Fixture,
) -> None:
    features = [pb.FeatureEvents()]
    features[0].event.add().new_value.id = "wifi"
    _list_hypothesis_fields.return_value = iter([("feature", features)])

    _update_hypothesis(pb.Hypothesis(), pb.EventRefuse(), "user_id")

    f._add_verdict_event.assert_called_once_with(
        features[0], pb.EventRefuse(), "user_id"
    )


def test_update_attribute_history__given_rejected_verdict__updates_history() -> None:
    emails = pb.Emails(email=[pb.Email(value="email")])
    emails_info = pb.EmailsInfo(value=emails)

    hypothesis_events = pb.EmailsEvents()
    hypothesis_events.event.add(
        metadata=pb.EventMetadata(rejected=pb.AttributeReject(), timestamp=111)
    )
    hypothesis_id = "1"
    revision_id = 2

    _update_attribute_history(
        emails_info, hypothesis_events, hypothesis_id, revision_id
    )

    assert emails_info.value == emails
    assert len(emails_info.change_history) == 1
    assert emails_info.change_history[0].change.hypothesis_id == hypothesis_id
    assert emails_info.change_history[0].change.events == hypothesis_events
    assert emails_info.change_history[0].revision_id == revision_id


def test_update_attribute_history__given_accepted_verdict__updates_value_and_history() -> None:
    info = pb.EmailsInfo()
    events = pb.EmailsEvents()
    events.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept(), timestamp=111),
        new_value=pb.Emails(email=[pb.Email(value="new value")]),
    )

    hypothesis_id = "1"
    revision_id = 2

    _update_attribute_history(info, events, hypothesis_id, revision_id)

    assert info.value == events.event[0].new_value
    assert info.metadata.approval_timestamp == 111
    assert info.metadata.revision_id == revision_id
    assert len(info.change_history) == 1
    assert info.change_history[0].revision_id == revision_id


def test_update_attribute_history__given_accepted_without_new_value__raises() -> None:
    info = pb.EmailsInfo()
    events = pb.EmailsEvents()
    events.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept(), timestamp=111),
    )

    hypothesis_id = "1"
    revision_id = 2

    with pytest.raises(Exception):
        _update_attribute_history(info, events, hypothesis_id, revision_id)


@pytest.mark.parametrize(
    "business",
    [
        pb.BusinessInternal(
            feature=[pb.FeatureInfo(value=pb.Feature(id="wifi"))]
        ),
        pb.BusinessInternal(),
    ],
)
def test_update_features_history__given_feature_events__updates(
    business: pb.BusinessInternal,
) -> None:
    feature_events = pb.FeatureEvents()
    feature_events.event.add().new_value.id = "wifi"

    hypothesis_id = "1"

    _update_features_history(
        business.feature, [feature_events], hypothesis_id, 1
    )

    history = business.feature[0].change_history
    assert len(history) == 1
    assert history[0].change.hypothesis_id == hypothesis_id
    assert history[0].change.events == feature_events


def test_update_features_history__given_feature_events_without_id__does_nothing() -> None:
    business = pb.BusinessInternal()

    feature_events = pb.FeatureEvents()
    feature_events.event.add()

    _update_features_history(business.feature, [feature_events], "1", 1)

    assert len(business.feature) == 0


def test_update_org_history__given_georeference__does_nothing(
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal().SerializeToString()

    hypothesis = pb.Hypothesis(hypothesis_id="1")
    hypothesis.edit_company.georeference.hypothesis.event.add()

    _update_org_history(f.session, "permalink", hypothesis)

    assert f.org_info.info == pb.BusinessInternal().SerializeToString()


def test_update_org_history__given_not_accepted_attribute_field__updates_it(
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal().SerializeToString()

    hypothesis = pb.Hypothesis(hypothesis_id="1")
    events = hypothesis.edit_company.company_state.hypothesis
    events.event.add(
        metadata=pb.EventMetadata(
            rejected=pb.AttributeReject(),
            timestamp=111,
        ),
    )

    _update_org_history(f.session, "permalink", hypothesis)

    result = pb.BusinessInternal.FromString(f.org_info.info).company_state
    assert len(result.change_history) == 1
    assert (
        result.change_history[0].change.hypothesis_id
        == hypothesis.hypothesis_id
    )
    assert result.change_history[0].change.events == events


def test_update_org_history__given_features__updates_them(
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal().SerializeToString()

    hypothesis = pb.Hypothesis(hypothesis_id="1")
    feature_events = hypothesis.edit_company.feature.add().hypothesis
    feature_events.event.add(
        metadata=pb.EventMetadata(
            duplicate=pb.AttributeDuplicate(),
            timestamp=1111,
        ),
        new_value=pb.Feature(id="wifi"),
    )
    feature_events.event.add(
        metadata=pb.EventMetadata(
            rejected=pb.AttributeReject(),
            timestamp=1111,
        ),
    )

    _update_org_history(f.session, "permalink", hypothesis)

    result = pb.BusinessInternal.FromString(f.org_info.info).feature
    assert len(result) == 1
    assert len(result[0].change_history) == 1
    assert (
        result[0].change_history[0].change.hypothesis_id
        == hypothesis.hypothesis_id
    )
    assert result[0].change_history[0].change.events == feature_events


def test_list_hypothesis_fields__for_new_company_singular_field__yields_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.new_company.emails_events.event.add()

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 1
    name, value = result[0]
    assert name == "emails"
    assert value == hypothesis.new_company.emails_events


def test_list_hypothesis_fields__for_new_company_repeated_field__yields_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.new_company.feature_events.add().event.add().new_value.id = (
        "wifi"
    )
    hypothesis.new_company.feature_events.add().event.add().new_value.id = (
        "price"
    )

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 1
    name, value = result[0]
    assert name == "feature"
    assert value == [it for it in hypothesis.new_company.feature_events]


def test_list_hypothesis_fields__for_new_company_duplicate_candidate__skips_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.new_company.duplicate_candidate.add()

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 0


def test_list_hypothesis_fields__for_edit_company_singular_field__yields_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add()

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 1
    name, value = result[0]
    assert name == "emails"
    assert value == hypothesis.edit_company.emails.hypothesis


def test_list_hypothesis_fields__for_edit_company_repeated_field__yields_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.feature.add().hypothesis.event.add().new_value.id = (
        "wifi"
    )
    hypothesis.edit_company.feature.add().hypothesis.event.add().new_value.id = (
        "price"
    )

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 1
    name, value = result[0]
    assert name == "feature"
    assert value == [it.hypothesis for it in hypothesis.edit_company.feature]


def test_list_hypothesis_fields__for_edit_company_company_id__skips_it() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.company_id = "1"

    result = list(_list_hypothesis_fields(hypothesis))

    assert len(result) == 0


@pytest.mark.parametrize(
    "verdict,field_name",
    [
        (pb.AttributeVerdict(accept=pb.AttributeAccept()), "accepted"),
        (pb.AttributeVerdict(reject=pb.AttributeReject()), "rejected"),
        (pb.AttributeVerdict(duplicate=pb.AttributeDuplicate()), "duplicate"),
        (
            pb.AttributeVerdict(need_info=pb.AttributeNeedInfo()),
            "validation_requested",
        ),
    ],
)
def test_to_event_metadata__given_verdict__returns_correct_metadata(
    verdict: pb.AttributeVerdict, field_name: Any
) -> None:
    metadata = _to_event_metadata(verdict, "user_id")

    assert metadata.HasField(field_name)


def test_to_event_metadata__always__sets_other_fields() -> None:
    verdict = pb.AttributeVerdict(
        accept=pb.AttributeAccept(),
        message="message",
    )

    metadata = _to_event_metadata(verdict, "user_id")

    assert metadata.HasField("timestamp")
    assert metadata.message == "message"
    assert metadata.source == pb.EventSource.CONTENT_MANAGER
    assert metadata.content_manager.login == "user_id"


def test_apply_attribute_verdict__for_valid_attribute__applies_it() -> None:
    hypothesis = pb.HypothesisEmails()

    verdict = pb.EmailsVerdict(
        verdict=pb.AttributeVerdict(accept=pb.AttributeAccept()),
        value=pb.Emails(email=[pb.Email(value="new email")]),
    )

    _apply_attribute_verdict(hypothesis, verdict, "user_id")

    assert len(hypothesis.hypothesis.event) == 1
    assert hypothesis.hypothesis.event[0].new_value == verdict.value
    assert hypothesis.hypothesis.event[0].metadata.HasField("accepted")


def test_apply_attribute_verdict__for_accepted_attribute_without_value__raises() -> None:
    hypothesis = pb.HypothesisEmails()

    verdict = pb.EmailsVerdict(
        verdict=pb.AttributeVerdict(accept=pb.AttributeAccept())
    )

    with pytest.raises(InvalidDataError):
        _apply_attribute_verdict(hypothesis, verdict, "user_id")


def make_feature_verdict(feature_id: str) -> pb.HypothesisVerdict:
    verdict = pb.HypothesisVerdict()
    feature_verdict = verdict.feature_id_to_verdict[feature_id]
    feature_verdict.value.value.bool_feature.value = True
    feature_verdict.value.id = feature_id
    feature_verdict.verdict.accept.CopyFrom(pb.AttributeAccept())
    return verdict


@mock.patch(f"{PATH}._apply_attribute_verdict")
def test_apply_feature_verdict__for_feature_with_hypothesis__applies_it(
    _apply_attribute_verdict: mock.Mock,
) -> None:
    feature_id = "wifi"

    hypothesis = pb.EditCompanyHypothesis()
    hypothesis.feature.add().hypothesis.event.add().new_value.id = feature_id

    verdict = make_feature_verdict(feature_id)

    _apply_feature_verdict(
        hypothesis.feature, verdict.feature_id_to_verdict, "user_id"
    )

    _apply_attribute_verdict.assert_called_once_with(
        hypothesis.feature[0],
        verdict.feature_id_to_verdict[feature_id],
        "user_id",
    )


@mock.patch(f"{PATH}._apply_attribute_verdict")
def test_apply_feature_verdict__for_feature_without_hypothesis__applies_it(
    _apply_attribute_verdict: mock.Mock,
) -> None:
    feature_id = "wifi"

    hypothesis = pb.EditCompanyHypothesis()
    hypothesis.feature.add().value.id = feature_id

    verdict = make_feature_verdict(feature_id)

    _apply_feature_verdict(
        hypothesis.feature, verdict.feature_id_to_verdict, "user_id"
    )

    _apply_attribute_verdict.assert_called_once_with(
        hypothesis.feature[0],
        verdict.feature_id_to_verdict[feature_id],
        "user_id",
    )


@mock.patch(f"{PATH}._apply_attribute_verdict")
def test_apply_feature_verdict__given_verdict_for_new_feature__applies_it(
    _apply_attribute_verdict: mock.Mock,
) -> None:
    feature_id = "wifi"

    hypothesis = pb.EditCompanyHypothesis()
    verdict = make_feature_verdict(feature_id)

    _apply_feature_verdict(
        hypothesis.feature, verdict.feature_id_to_verdict, "user_id"
    )

    _apply_attribute_verdict.assert_called_once_with(
        pb.HypothesisFeature(),
        verdict.feature_id_to_verdict[feature_id],
        "user_id",
    )


@mock.patch(f"{PATH}._apply_feature_verdict")
def test_apply_verdicts__given_features__applies_features(
    _apply_feature_verdict: mock.Mock,
) -> None:
    feature_id = "wifi"
    hypothesis = pb.EditCompanyHypothesis()
    verdict = make_feature_verdict(feature_id)

    _apply_verdicts(hypothesis, verdict, "user_id")

    _apply_feature_verdict.assert_called_once_with(
        hypothesis.feature,
        verdict.feature_id_to_verdict,
        "user_id",
    )


@mock.patch(f"{PATH}._apply_attribute_verdict")
def test_apply_verdicts__given_attributes__applies_verdict(
    _apply_attribute_verdict: mock.Mock,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()

    verdict = pb.HypothesisVerdict()
    verdict.emails.value.email.add(value="email")
    verdict.emails.verdict.accept.CopyFrom(pb.AttributeAccept())

    _apply_verdicts(hypothesis, verdict, "user_id")

    _apply_attribute_verdict.assert_called_once_with(
        hypothesis.emails,
        verdict.emails,
        "user_id",
    )


def test_is_attribute_in_state__given_empty_events__returns_false() -> None:
    events = pb.EmailsEvents()

    assert _is_attribute_in_state(events, {"rejected"}) is False


def test_is_attribute_in_state__for_attribute_in_state__returns_true() -> None:
    events = pb.EmailsEvents()
    events.event.add().metadata.rejected.CopyFrom(pb.AttributeReject())

    assert _is_attribute_in_state(events, {"rejected"}) is True


def test_is_attribute_in_state__for_attribute_not_in_state__returns_false() -> None:
    events = pb.EmailsEvents()
    events.event.add().metadata.rejected.CopyFrom(pb.AttributeReject())

    assert _is_attribute_in_state(events, {"accepted"}) is False


def test_is_hypothesis_in_state__when_all_attributes_in_state__returns_true() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=pb.EventMetadata(rejected=pb.AttributeReject())
    )
    hypothesis.edit_company.feature.add().hypothesis.event.add(
        metadata=pb.EventMetadata(rejected=pb.AttributeReject())
    )

    assert _is_hypothesis_in_state(hypothesis, {"rejected"}) is True


def test_is_hypothesis_in_state__when_some_attributes_not_in_state__returns_false() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=pb.EventMetadata(rejected=pb.AttributeReject())
    )
    hypothesis.edit_company.links.hypothesis.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept())
    )

    assert _is_hypothesis_in_state(hypothesis, {"rejected"}) is False


def test_is_validation_requested__given_validation_requested_event__returns_true() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=pb.EventMetadata(validation_requested=pb.AttributeNeedInfo())
    )
    hypothesis.edit_company.links.hypothesis.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept())
    )

    assert _is_validation_requested(hypothesis) is True


def test_is_validation_requested__given_no_validation_requested_event__returns_false() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.links.hypothesis.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept())
    )

    assert _is_validation_requested(hypothesis) is False


def test_build_business_diff__given_approved_emails__adds_them() -> None:
    business = pb.BusinessInternal()
    business.emails.value.email.add(value="email")
    business.emails.metadata.approval_timestamp = 11111

    georef = pb.GeoreferenceEvents()

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert result.emails.value == business.emails.value
    assert (
        result.emails.approval_timestamp
        == business.emails.metadata.approval_timestamp
    )


def test_build_business_diff__given_approved_feature__adds_it() -> None:
    business = pb.BusinessInternal()
    feature = business.feature.add()
    feature.value.id = "wifi"
    feature.metadata.approval_timestamp = 11111

    georef = pb.GeoreferenceEvents()

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert len(result.feature) == 1
    assert result.feature[0].value == feature.value
    assert (
        result.feature[0].approval_timestamp
        == feature.metadata.approval_timestamp
    )


def test_build_business_diff__given_unapproved_emails__doesnt_add() -> None:
    business = pb.BusinessInternal()
    business.emails.value.email.add(value="email")

    georef = pb.GeoreferenceEvents()

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert not result.HasField("emails")


def test_build_business_diff__given_unapproved_feature__doesnt_add() -> None:
    business = pb.BusinessInternal()
    feature = business.feature.add()
    feature.value.id = "wifi"

    georef = pb.GeoreferenceEvents()

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert not result.feature


def test_build_business_diff__given_accepted_georef__adds_it() -> None:
    business = pb.BusinessInternal()

    georef = pb.GeoreferenceEvents()
    event = georef.event.add(
        metadata=pb.EventMetadata(
            accepted=pb.AttributeAccept(),
            timestamp=1111,
        )
    )
    event.new_value.point.lon = 37.6
    event.new_value.point.lat = 55.7

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert result.georeference.value == event.new_value
    assert result.georeference.approval_timestamp == event.metadata.timestamp


def test_build_business_diff__given_accepted_georef_without_new_value__raises() -> None:
    business = pb.BusinessInternal()

    georef = pb.GeoreferenceEvents()
    georef.event.add(
        metadata=pb.EventMetadata(
            accepted=pb.AttributeAccept(),
            timestamp=1111,
        )
    )

    with pytest.raises(Exception):
        _build_business_diff("permalink", ["signal"], business, georef)


def test_build_business_diff__given_rejected_georef__doesnt_add() -> None:
    business = pb.BusinessInternal()

    georef = pb.GeoreferenceEvents()
    georef.event.add(
        metadata=pb.EventMetadata(
            rejected=pb.AttributeReject(),
            timestamp=1111,
        )
    )

    result = _build_business_diff("permalink", ["signal"], business, georef)

    assert not result.HasField("georeference")


def test_build_business_diff__always__sets_company_id_and_signal_id() -> None:
    result = _build_business_diff(
        "permalink",
        ["sid1", "sid2"],
        pb.BusinessInternal(),
        pb.GeoreferenceEvents(),
    )

    assert result == pb.BusinessDiff(
        company_id="permalink", signal_id=["sid1", "sid2"]
    )


def validation_requested_metadata(
    sources: list["pb.ValidationSource.V"],
) -> pb.EventMetadata:
    return pb.EventMetadata(
        validation_requested=pb.AttributeNeedInfo(
            request=[
                pb.AttributeNeedInfo.ValidationRequest(
                    source=source,
                )
                for source in sources
            ]
        )
    )


def check_validation_unsuccessful(
    event: Any, source: "pb.ValidationSource.V"
) -> None:
    assert event.metadata.HasField("validation_completed")
    assert (
        event.metadata.validation_completed.status
        == pb.ValidationCompleted.Status.UNSUCCESSFUL
    )
    assert event.metadata.source == pb.EventSource.VALIDATION
    assert event.metadata.validator.source == source


def test_add_unsuccessful_validation__given_several_sources__adds_events_for_all() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=validation_requested_metadata(
            [
                pb.ValidationSource.CALL_CENTER,
                pb.ValidationSource.TOLOKA,
            ]
        )
    )

    _add_unsuccessful_validation(hypothesis)

    events = hypothesis.edit_company.emails.hypothesis.event
    assert len(events) == 4
    check_validation_unsuccessful(events[1], pb.ValidationSource.CALL_CENTER)
    check_validation_unsuccessful(events[2], pb.ValidationSource.TOLOKA)

    assert events[3].metadata.HasField("verdict_required")
    assert events[3].metadata.source == pb.EventSource.SPS
    assert events[3].metadata.HasField("sps")


def test_add_unsuccessful_validation__given_no_validation_requested__does_nothing() -> None:
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=pb.EventMetadata(accepted=pb.AttributeAccept(), timestamp=111)
    )

    _add_unsuccessful_validation(hypothesis)

    assert len(hypothesis.edit_company.emails.hypothesis.event) == 1


def test_process_validation_requested__given_several_validation_sources__creates_task_for_each(
    f: Fixture,
) -> None:
    f.hypothesis.org_info = db.OrgInfo(info=b"")
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=validation_requested_metadata(
            [pb.ValidationSource.CALL_CENTER]
        )
    )
    hypothesis.edit_company.links.hypothesis.event.add(
        metadata=validation_requested_metadata([pb.ValidationSource.TOLOKA]),
    )
    hypothesis.edit_company.rubrics.hypothesis.event.add(
        metadata=validation_requested_metadata(
            [pb.ValidationSource.CALL_CENTER]
        )
    )

    _process_validation_requested(f.session, f.hypothesis, hypothesis)

    assert f.hypothesis.status == db.HypothesisStatus.need_info
    f.task_creator.create_validation_task.call_count == 2
    f.task_creator.create_validation_task.assert_any_call(
        pb.ValidationSource.CALL_CENTER, mock.ANY, mock.ANY
    )
    f.task_creator.create_validation_task.assert_any_call(
        pb.ValidationSource.TOLOKA, mock.ANY, mock.ANY
    )
    f.session.add_validation_tasks.assert_called_once()


def test_process_validation_requested__when_task_creator_throws__calls_add_unsuccessful_validation(
    f: Fixture,
) -> None:
    f.hypothesis.org_info = db.OrgInfo(info=b"")
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=validation_requested_metadata(
            [pb.ValidationSource.CALL_CENTER]
        )
    )
    f.task_creator.create_validation_task.side_effect = Exception()

    _process_validation_requested(f.session, f.hypothesis, hypothesis)

    f._add_unsuccessful_validation.assert_called_once_with(hypothesis)
    f.logger.exception.assert_called_once()
    f.session.add_validation_tasks.assert_not_called()


def test_process_validation_requested__when_task_creator_returns_none__calls_add_unsuccessful_validation(
    f: Fixture,
) -> None:
    f.hypothesis.org_info = db.OrgInfo(info=b"")
    hypothesis = pb.Hypothesis()
    hypothesis.edit_company.emails.hypothesis.event.add(
        metadata=validation_requested_metadata(
            [pb.ValidationSource.CALL_CENTER]
        )
    )
    f.task_creator.create_validation_task.return_value = None

    _process_validation_requested(f.session, f.hypothesis, hypothesis)

    f._add_unsuccessful_validation.assert_called_once_with(hypothesis)
    f.logger.error.assert_called_once()
    f.session.add_validation_tasks.assert_not_called()


@mock.patch(f"{PATH}._update_org_history")
def test_process_edit_verdict__when_all_attributes_are_rejected__rejects_signals(
    _: mock.Mock,
    f: Fixture,
) -> None:
    f.hypothesis.org_id = "org id"
    f.hypothesis.hypothesis = pb.Hypothesis(
        hypothesis_id=f.hypothesis.id,
        edit_company=pb.EditCompanyHypothesis(company_id="permalink"),
    ).SerializeToString()

    verdict = pb.HypothesisVerdict()
    verdict.emails.verdict.reject.CopyFrom(pb.AttributeReject())

    process_edit_verdict(f.session, f.hypothesis, verdict, "user_id")

    assert f.hypothesis.status == db.HypothesisStatus.rejected
    f.session.update_signal_status.assert_called_once_with(
        f.hypothesis.id, pb.SignalResult(rejected=pb.SignalReject())
    )


@pytest.mark.parametrize(
    "verdict",
    [
        pb.HypothesisVerdict(
            emails=pb.EmailsVerdict(
                verdict=pb.AttributeVerdict(duplicate=pb.AttributeDuplicate())
            ),
            georeference=pb.GeoreferenceVerdict(
                verdict=pb.AttributeVerdict(reject=pb.AttributeReject())
            ),
        ),
        pb.HypothesisVerdict(
            georeference=pb.GeoreferenceVerdict(
                verdict=pb.AttributeVerdict(duplicate=pb.AttributeDuplicate())
            ),
            emails=pb.EmailsVerdict(
                verdict=pb.AttributeVerdict(reject=pb.AttributeReject())
            ),
        ),
        pb.HypothesisVerdict(
            feature_id_to_verdict={
                "wifi": pb.FeatureVerdict(
                    verdict=pb.AttributeVerdict(
                        duplicate=pb.AttributeDuplicate()
                    ),
                    value=pb.Feature(id="wifi"),
                ),
                "price": pb.FeatureVerdict(
                    verdict=pb.AttributeVerdict(
                        reject=pb.AttributeReject(),
                    ),
                    value=pb.Feature(id="price"),
                ),
            }
        ),
    ],
)
def test_process_edit_verdict__for_unapproved_duplicate__creates_diff(
    verdict: pb.HypothesisVerdict,
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal().SerializeToString()
    f.hypothesis.org_id = "org id"
    f.hypothesis.hypothesis = pb.Hypothesis(
        hypothesis_id=f.hypothesis.id,
        edit_company=pb.EditCompanyHypothesis(company_id="permalink"),
    ).SerializeToString()
    f.session.signals.return_value = [db.Signal(id="id")]

    process_edit_verdict(f.session, f.hypothesis, verdict, "user_id")

    assert f.hypothesis.status == db.HypothesisStatus.ready_for_sprav
    diff = f.session.session.add.call_args.args[0]
    assert isinstance(diff, db.Diff)


@pytest.mark.parametrize(
    "verdict",
    [
        pb.HypothesisVerdict(
            emails=pb.EmailsVerdict(
                verdict=pb.AttributeVerdict(duplicate=pb.AttributeDuplicate())
            ),
        ),
        pb.HypothesisVerdict(
            feature_id_to_verdict={
                "wifi": pb.FeatureVerdict(
                    verdict=pb.AttributeVerdict(
                        duplicate=pb.AttributeDuplicate()
                    ),
                    value=pb.Feature(id="wifi"),
                ),
            }
        ),
    ],
)
def test_process_edit_verdict__when_attribute_is_approved_duplicate__creates_diff(
    verdict: pb.HypothesisVerdict,
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal(
        emails=pb.EmailsInfo(
            metadata=pb.MetadataInternal(
                approval_timestamp=1111,
                revision_id=1,
            )
        ),
        feature=[
            pb.FeatureInfo(
                value=pb.Feature(id="wifi"),
                metadata=pb.MetadataInternal(
                    approval_timestamp=1111,
                    revision_id=1,
                ),
            ),
        ],
    ).SerializeToString()

    f.hypothesis.org_id = "org id"
    f.hypothesis.hypothesis = pb.Hypothesis(
        hypothesis_id=f.hypothesis.id,
        edit_company=pb.EditCompanyHypothesis(company_id="permalink"),
    ).SerializeToString()
    f.session.signals.return_value = [db.Signal(id="id")]

    process_edit_verdict(f.session, f.hypothesis, verdict, "user_id")

    diff = f.session.session.add.call_args.args[0]
    assert isinstance(diff, db.Diff)
    assert f.hypothesis.status == db.HypothesisStatus.ready_for_sprav


def test_process_edit_verdict__when_attribute_is_accepted__creates_diff(
    f: Fixture,
) -> None:
    f.org_info.info = pb.BusinessInternal().SerializeToString()
    f.hypothesis.org_id = "org id"
    f.hypothesis.hypothesis = pb.Hypothesis(
        hypothesis_id=f.hypothesis.id,
        edit_company=pb.EditCompanyHypothesis(company_id="permalink"),
    ).SerializeToString()

    verdict = pb.HypothesisVerdict()
    verdict.emails.verdict.accept.CopyFrom(pb.AttributeAccept())
    verdict.emails.value.email.add(value="email")

    signal = db.Signal(id="signal id", permalink="permalink")
    f.session.signals.return_value = [signal]

    process_edit_verdict(f.session, f.hypothesis, verdict, "user_id")

    diff = f.session.session.add.call_args.args[0]
    assert isinstance(diff, db.Diff)


@mock.patch(f"{PATH}._process_validation_requested")
def test_process_edit_verdict__when_validation_requested__calls_process_validation_requested(
    _process_validation_requested: mock.Mock,
    f: Fixture,
) -> None:
    f.hypothesis.org_id = "org id"
    f.hypothesis.hypothesis = pb.Hypothesis(
        hypothesis_id=f.hypothesis.id,
        edit_company=pb.EditCompanyHypothesis(company_id="permalink"),
    ).SerializeToString()

    verdict = pb.HypothesisVerdict()
    verdict.emails.verdict.need_info.CopyFrom(pb.AttributeNeedInfo())

    process_edit_verdict(f.session, f.hypothesis, verdict, "user_id")

    _process_validation_requested.assert_called_once()
