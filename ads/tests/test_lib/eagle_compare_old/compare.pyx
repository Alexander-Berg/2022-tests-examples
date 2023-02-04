from ads.bsyeti.libs.py_tnode.node cimport NodeRef, TNode
from compare cimport FastTNodeSort


def fast_node_sort(obj):
    assert isinstance(obj, NodeRef) and obj.IsList()
    FastTNodeSort((<NodeRef>obj).node[0])

def fast_ki_group(obj):
    assert isinstance(obj, NodeRef) and obj.IsList()
    FastKiGroup((<NodeRef>obj).node[0])

