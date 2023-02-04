from ads.bsyeti.libs.py_tnode.node cimport TNode


cdef extern from "ads/bsyeti/tests/test_lib/eagle_compare_old/compare.h" namespace "NEagleCompare" nogil:
    void FastTNodeSort(TNode&)
    void FastKiGroup(TNode&)
