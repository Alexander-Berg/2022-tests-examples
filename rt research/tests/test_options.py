import irt.broadmatching.common_options


def test_not_empty():
    options = irt.broadmatching.common_options.get_options()
    assert isinstance(options, dict)
    assert len(options) > 10
    assert 'dyn_tasks_source_table' in options
    assert 'DictNorm' in options
    assert 'Monitor' in options
    assert 'minicategs' in options

    assert options['SpamPhrases_params']['spam_phrases_wide_tr'] == '/home/some_user/arcadia/rt-research/broadmatching/dicts//dict_wide_spam_phrases_tr'
