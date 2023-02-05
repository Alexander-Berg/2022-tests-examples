package com.yandex.mail.service.work

import androidx.work.Data
import com.yandex.mail.BaseMailApplication
import com.yandex.mail.metrica.YandexMailMetrica
import io.reactivex.Scheduler

class TestDataManagingExecutor(
    context: BaseMailApplication,
    metrica: YandexMailMetrica,
    scheduler: Scheduler
) : DataManagingExecutor(context, metrica, scheduler) {

    private val works: MutableList<Data> = mutableListOf()

    fun getWorks(): List<Data> = works

    fun getWorks(filter: (Data) -> Boolean): List<Data> {
        return works.filter(filter)
    }

    override fun doWork(data: Data) {
        super.doWork(data)
        works.add(data)
    }
}
