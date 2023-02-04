from pytorch_embedding_model.cpp_lib import cpp_module_arcadia


def test_deleter_gc():
    def _fn():
        string_processor = cpp_module_arcadia.TInfiniteStringProcessingUnit()
        for i in range(20):
            string_processor.AddString((chr(i) * 100000).encode("utf-8"))
    # This would segfault if we have not passed ownership properly
    _fn()
