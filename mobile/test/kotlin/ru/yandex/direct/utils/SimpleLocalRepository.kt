// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.utils

import ru.yandex.direct.repository.base.BaseLocalRepository

class SimpleLocalRepository<TQuery, TEntity>(private val select: (TQuery, TEntity?) -> TEntity)
        : BaseLocalRepository<TQuery, TEntity> {

    private var savedEntity: TEntity? = null

    override fun select(query: TQuery) = select(query, savedEntity)

    override fun update(query: TQuery, entities: TEntity) {
        savedEntity = entities
    }

    override fun containsActualData(query: TQuery) = savedEntity != null
}
