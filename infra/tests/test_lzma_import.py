def test_import_lzma_ok():
    try:
        import lzma
        assert lzma.open is not None
    except Exception as e:
        assert False, f'unexpected exception while importing lzma: {e}'
