import pytest
from mdh.core.management.commands.resyncrecords import Command as ResyncCommand, CommandError


def test_resync(init_user, init_lock):

    init_user(robot=True)

    def resync():
        ResyncCommand().handle(
            domains='one, two',
            references=' three, four',
            records='2075b52d-4083-4ee6-a30a-7c3b79e9b3ac,2075b52d-4083-4ee6-a30a-7c3b79e9b3a1'
        )

    with pytest.raises(CommandError):
        resync()

    lock = init_lock('logbroker_send_record', result='{}')

    resync()

    lock.refresh_from_db()
    assert lock.result == (
        '{"domains": ["one", "two"], "references": ["three", "four"], '
        '"records": ["2075b52d-4083-4ee6-a30a-7c3b79e9b3ac", "2075b52d-4083-4ee6-a30a-7c3b79e9b3a1"], "attrs": {}}'
    )
