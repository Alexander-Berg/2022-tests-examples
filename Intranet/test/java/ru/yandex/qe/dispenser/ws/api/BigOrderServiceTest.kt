package ru.yandex.qe.dispenser.ws.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase
import ru.yandex.qe.dispenser.ws.bot.BigOrderDto
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager
import ru.yandex.qe.dispenser.ws.bot.BigOrdersPageDto
import ru.yandex.qe.dispenser.ws.bot.CreateBigOrderDto
import ru.yandex.qe.dispenser.ws.bot.UpdateBigOrderDto
import java.time.LocalDate

class BigOrderServiceTest(
    @Autowired private val bigOrderManager: BigOrderManager
): AcceptanceTestBase() {

    @BeforeEach
    fun beforeEachTest() {
        bigOrderManager.clear()
    }

    @Test
    fun create() {
        val createRequest = CreateBigOrderDto(LocalDate.now())
        val createdBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequest,  BigOrderDto::class.java)
        Assertions.assertEquals(createRequest.date, createdBigOrder.date)
    }

    @Test
    fun update() {
        val createRequest = CreateBigOrderDto(LocalDate.now())
        val createdBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequest,  BigOrderDto::class.java)
        val updateRequest = UpdateBigOrderDto(LocalDate.now().plusDays(10))
        val updatedBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders/" + createdBigOrder.id)
            .put(updateRequest,  BigOrderDto::class.java)
        Assertions.assertEquals(updateRequest.date, updatedBigOrder.date)
    }

    @Test
    fun delete() {
        val createRequest = CreateBigOrderDto(LocalDate.now())
        val createdBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequest,  BigOrderDto::class.java)
        val deleteResponse = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders/" + createdBigOrder.id)
            .delete()
        Assertions.assertEquals(204, deleteResponse.status)
    }

    @Test
    fun getById() {
        val createRequest = CreateBigOrderDto(LocalDate.now())
        val createdBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequest,  BigOrderDto::class.java)
        val foundBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders/" + createdBigOrder.id)
            .get(BigOrderDto::class.java)
        Assertions.assertEquals(createdBigOrder, foundBigOrder)
        val notFoundBigOrder = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders/" + createdBigOrder.id + 1)
            .get()
        Assertions.assertEquals(404, notFoundBigOrder.status)
    }

    @Test
    fun getPage() {
        val createRequestOne = CreateBigOrderDto(LocalDate.now())
        val createRequestTwo = CreateBigOrderDto(LocalDate.now().plusDays(1))
        val createdBigOrderOne = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequestOne,  BigOrderDto::class.java)
        val createdBigOrderTwo = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .post(createRequestTwo,  BigOrderDto::class.java)
        val foundBigOrderFirstPage = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .replaceQueryParam("limit", 1)
            .get(BigOrdersPageDto::class.java)
        val foundBigOrderSecondPage = createAuthorizedLocalClient(AMOSOV_F)
            .path("/v1/big-orders")
            .replaceQueryParam("limit", 1)
            .replaceQueryParam("from", foundBigOrderFirstPage.nextFrom!!)
            .get(BigOrdersPageDto::class.java)
        Assertions.assertEquals(createdBigOrderOne, foundBigOrderFirstPage.bigOrders[0])
        Assertions.assertEquals(createdBigOrderTwo, foundBigOrderSecondPage.bigOrders[0])
    }

}
