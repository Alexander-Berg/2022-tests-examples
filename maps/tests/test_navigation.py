from yandex.maps.proto.factory import navigation_pb2
from yandex.maps.proto.factory import release_pb2

from common import canonical_message


def test_tree_node():
    msg = navigation_pb2.TreeNode()
    msg.id = "releases/draft/1"
    msg.name = "test_release"
    msg.hasChildren = True
    msg.release.id = "1"
    msg.release.name = "test_release"
    msg.release.currentStatus = release_pb2.Release.DRAFT
    msg.release.targetStatus = release_pb2.Release.TESTING
    msg.release.etag = "abc"

    return canonical_message(msg, 'tree_node')


def test_returning_subtree():

    msg = navigation_pb2.TreeNodes()
    release_msg = msg.nodes.add()
    release_msg.id = "releases"
    release_msg.name = "Releases"
    release_msg.hasChildren = True

    draft_status_node = release_msg.childrenNodes.add()
    draft_status_node.id = "releases/draft"
    draft_status_node.name = "Draft"
    draft_status_node.hasChildren = True

    release_node = draft_status_node.childrenNodes.add()

    release_node.id = "releases/draft/1"
    release_node.name = "test_release"
    release_node.hasChildren = True
    release_node.release.id = "1"
    release_node.release.name = "test_release"
    release_node.release.currentStatus = release_pb2.Release.DRAFT
    release_node.release.targetStatus = release_pb2.Release.TESTING
    release_node.release.etag = "abc"

    production_status_node = release_msg.childrenNodes.add()
    production_status_node.id = "releases/production"
    production_status_node.name = "production"
    production_status_node.hasChildren = True

    delivery_msg = msg.nodes.add()
    delivery_msg.id = "delivery"
    delivery_msg.name = "Delivery"
    delivery_msg.hasChildren = True

    return canonical_message(msg, 'tree_node')
