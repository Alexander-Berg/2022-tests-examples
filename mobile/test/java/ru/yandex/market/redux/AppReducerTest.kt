package ru.yandex.market.redux

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.action.BatchAction
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.base.redux.stateobject.asStateObject
import ru.yandex.market.clean.domain.model.checkout.checkoutCommonUserInputTestInstance
import ru.yandex.market.clean.domain.model.usercontact.UserContact
import ru.yandex.market.redux.actions.checkout.commonuserinput.commonInputAction_AddUserContactActionTestInstance
import ru.yandex.market.redux.actions.checkout.commonuserinput.commonInputAction_UserContactSelectedActionTestInstance
import ru.yandex.market.redux.reducers.AppReducer
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class AppReducerTest {

    private val appReducer = AppReducer()

    @Test
    fun `Batch action test`() {
        val action1 = commonInputAction_AddUserContactActionTestInstance()
        val action2 = commonInputAction_UserContactSelectedActionTestInstance(userContactId = action1.id)
        val batchAction = BatchAction(listOf(action1, action2))
        val state = AppState(
            CheckoutState(
                checkoutCommonUserInput = checkoutCommonUserInputTestInstance().asSingleStateObject(),
                userContacts = emptyMap<String, UserContact>().asStateObject()
            )
        )

        val reducedState = appReducer.reduce(state, batchAction)
        val reducedUserContact = reducedState.checkoutState.checkoutCommonUserInput.stateValue?.selectedUserContact
        val expectedUserContact = with(action1) {
            UserContact(
                id = id,
                fullName = fullName,
                phoneNum = phone,
                email = email
            )
        }

        assertThat(reducedUserContact).isEqualTo(expectedUserContact)
    }
}