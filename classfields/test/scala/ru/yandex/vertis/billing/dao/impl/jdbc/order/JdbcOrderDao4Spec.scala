package ru.yandex.vertis.billing.dao.impl.jdbc.order

import ru.yandex.vertis.billing.dao.{OrderDao, OrderDao4Spec}

/**
  * Runnable [[OrderDao4Spec]] implementation.
  */
class JdbcOrderDao4Spec extends OrderDao4Spec with JdbcOrderDaoSpecComponent
