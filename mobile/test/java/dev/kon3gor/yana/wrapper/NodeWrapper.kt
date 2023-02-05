package dev.kon3gor.yana.wrapper

import dev.kon3gor.yana.dsl.node.node
import dev.kon3gor.yana.mocks.Node

class NodeWrapper : Wrapper() {

    override val id = generateId()

    override fun build() = node<Node>(id)
}