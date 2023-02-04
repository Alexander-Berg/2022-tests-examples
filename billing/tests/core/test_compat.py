import luigi.notifications as note


def test_send_email():
    # функция с заплаткой вернёт True
    assert note.send_error_email('subj', 'sometext')
