package ru.yandex.market.test.util

fun <T> List<T>.mutate(block: ListMutator<T>.() -> Unit): List<T> {
    return ListMutator(this).apply(block).getList()
}

class ListMutator<T>(list: List<T>) {

    private val list: MutableList<T> = list.toMutableList()

    fun getList() = list.toList()

    fun mutateAt(index: Int, mutation: T.() -> T) {
        list[index] = list[index].run(mutation)
    }

    fun mutateFirst(mutation: T.() -> T) {
        mutateAt(0, mutation)
    }

    fun mutateLast(mutation: T.() -> T) {
        mutateAt(list.size - 1, mutation)
    }

    fun mutateAll(mutation: T.() -> T) {
        list.indices.forEach { mutateAt(it, mutation) }
    }
}