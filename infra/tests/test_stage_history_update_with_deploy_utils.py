from typing import List, Optional, Set, Tuple

from infra.deploy_notifications_controller.lib.models.action import DummyQnotifierMessage, \
    InfraChange, QnotifierMessage
from infra.deploy_notifications_controller.lib.models.stage import Stage
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryUpdate, ChangeKind
from test_stage_history_change_utils import default_old_spec_revision, default_old_du_revision, \
    default_update_du_revision, default_update_spec_revision, StageInfraParameters, default_infra_parameters, \
    create_stage_many_du, create_update, create_spec_many_du, create_status_many_du, create_input_data, default_event, \
    create_expected_message, default_update_spec_revision_delta, default_update_du_revision_delta


class TestStageDeployProgress:
    __slots__ = [
        '_spec_revision',
        '_du_revisions',
        '_du_ldrs',
    ]

    _spec_revision: int
    _du_revisions: List[int]
    _du_ldrs: List[Optional[int]]

    def __init__(
        self,
        spec_revision: int,
        du_revisions: List[int],
        du_ldrs: List[Optional[int]],
    ):
        self._spec_revision = spec_revision
        self._du_revisions = du_revisions
        self._du_ldrs = du_ldrs

    @staticmethod
    def all_du_same(
        spec_revision: int,
        du_revision: int,
        du_ldr: int,
        du_count: int,
    ):
        return TestStageDeployProgress(
            spec_revision=spec_revision,
            du_revisions=[du_revision] * du_count,
            du_ldrs=[du_ldr] * du_count,
        )

    @property
    def spec_revision(self) -> int:
        return self._spec_revision

    @property
    def du_revisions(self) -> List[int]:
        return self._du_revisions

    @property
    def du_ldrs(self) -> List[Optional[int]]:
        return self._du_ldrs

    def copy_with(
        self,
        spec_revision: Optional[int] = None,
        du_revisions: Optional[List[int]] = None,
        du_ldrs: Optional[List[Optional[int]]] = None,
    ):
        return TestStageDeployProgress(
            spec_revision=spec_revision or self.spec_revision,
            du_revisions=du_revisions or self.du_revisions,
            du_ldrs=du_ldrs or self.du_ldrs,
        )


def create_all_du_deployed_old_progress(
    du_count: int,
):
    return TestStageDeployProgress.all_du_same(
        spec_revision=default_old_spec_revision,
        du_revision=default_old_du_revision,
        du_ldr=default_old_du_revision,
        du_count=du_count,
    )


one_du_deployed_old_progress = create_all_du_deployed_old_progress(
    du_count=1,
)

two_du_deployed_old_progress = create_all_du_deployed_old_progress(
    du_count=2,
)


def create_all_du_deploying_after_progress(
    old_progress: TestStageDeployProgress,
):
    return old_progress.copy_with(
        spec_revision=old_progress.spec_revision + default_update_spec_revision_delta,
        du_revisions=list(map(
            lambda revision: revision + default_update_du_revision_delta,
            old_progress.du_revisions
        ))
    )


one_du_deploying_progress = create_all_du_deploying_after_progress(
    old_progress=one_du_deployed_old_progress,
)

two_du_deploying_progress = create_all_du_deploying_after_progress(
    old_progress=two_du_deployed_old_progress,
)


def create_all_du_deployed_after_progress(
    old_progress: TestStageDeployProgress,
):
    return old_progress.copy_with(
        du_ldrs=old_progress.du_revisions,
    )


one_du_deployed_updated_progress = create_all_du_deployed_after_progress(
    old_progress=one_du_deploying_progress,
)

two_du_deployed_updated_progress = create_all_du_deployed_after_progress(
    old_progress=two_du_deploying_progress,
)


class TestStageDeployParameters:
    __slots__ = [
        'old_stage_progress',
        'updated_stage_progress',
        'infra_parameters',
    ]

    old_stage_progress: TestStageDeployProgress
    updated_stage_progress: TestStageDeployProgress
    infra_parameters: StageInfraParameters

    def __init__(
        self,
        old_stage_progress: TestStageDeployProgress,
        updated_stage_progress: TestStageDeployProgress,
        infra_parameters: Optional[StageInfraParameters] = default_infra_parameters,
    ):
        self.old_stage_progress = old_stage_progress
        self.updated_stage_progress = updated_stage_progress
        self.infra_parameters = infra_parameters


def update_stage_deploy_scenario(
    parameters: TestStageDeployParameters,
    expected_message_generator,
    expected_infras_generator,
):
    old_stage_progress = parameters.old_stage_progress

    stage = create_stage_many_du(
        revision=old_stage_progress.spec_revision,
        du_revisions=old_stage_progress.du_revisions,
        du_ldrs=old_stage_progress.du_ldrs,
        infra_parameters=parameters.infra_parameters,
    )

    updated_stage_progress = parameters.updated_stage_progress

    update = create_update(
        spec=create_spec_many_du(
            revision=updated_stage_progress.spec_revision,
            du_revisions=updated_stage_progress.du_revisions,
        ),
        status=create_status_many_du(
            revision=updated_stage_progress.spec_revision,
            du_revisions=updated_stage_progress.du_revisions,
            du_ldrs=updated_stage_progress.du_ldrs,
        ),
    )

    input_data = create_input_data(stage)

    expected_message = expected_message_generator(
        update=update,
        stage=stage,
    )

    expected_infras = expected_infras_generator(
        update=update,
        stage=stage,
    )

    actual_message, output_data = update.process_changes(input_data)
    assert actual_message == expected_message
    assert output_data.infra_changes == expected_infras


def dummy_qnotifier_message_generator(
    update: StageHistoryUpdate,
    stage: Stage,
) -> QnotifierMessage:
    return DummyQnotifierMessage(
        stage_id=stage.id,
        timestamp=default_event.timestamp,
    )


def create_expected_message_generator(
    custom_change_kinds: Set[ChangeKind],
    revisions: Set[int],
):
    def expected_message_generator(
        update: StageHistoryUpdate,
        stage: Stage,
    ) -> QnotifierMessage:
        return create_expected_message(
            stage, update,
            custom_tags=[
                StageHistoryUpdate.create_change_tag('spec'),
            ],
            custom_change_kinds=custom_change_kinds,
            revisions=revisions,
        )

    return expected_message_generator


def create_expected_infras_generator(
    expected_spec_revisions_to_infras: List[Tuple[int, InfraChange.EventKind]],
):
    def expected_infras_generator(
        update: StageHistoryUpdate,
        stage: Stage,
    ):
        return [
            update.create_infra_change(
                revision=spec_revision,
                stage=stage,
                event_kind=event_kind,
            )
            for spec_revision, event_kind in expected_spec_revisions_to_infras
        ]

    return expected_infras_generator


expected_empty_infras_generator = create_expected_infras_generator([])
