package just.adapter.item

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CallbacksTest {

    @Test
    fun `'callAlways' calls actions`() {
        val actions = mock<TestActions>()
        val callbacks = callAlways(actions)
        callbacks.call { onTestAction() }
        verify(actions).onTestAction()
    }

    @Test
    fun `'callNever' do not throws exception on dispatch call`() {
        val callbacks = callNever<TestActions>()
        callbacks.call { onTestAction() }
    }

    @Test
    fun `'callWhen' calls actions if condition is met`() {
        val actions = mock<TestActions>()
        val callbacks = callWhen({ true }, actions)
        callbacks.call { onTestAction() }
        verify(actions).onTestAction()
    }

    @Test
    fun `'callWhen' do not calls actions if condition is not met`() {
        val actions = mock<TestActions>()
        val callbacks = callWhen({ false }, actions)
        callbacks.call { onTestAction() }
        verify(actions, never()).onTestAction()
    }

    @Test
    fun `'callWhen' checks condition on each dispatch`() {
        val condition = mock<() -> Boolean>()
        val actions = mock<TestActions>()
        val callbacks = callWhen(condition, actions)

        whenever(condition.invoke()) doReturn false
        verify(condition, never()).invoke()
        callbacks.call { onTestAction() }
        verify(condition, times(1)).invoke()
        verify(actions, never()).onTestAction()

        whenever(condition.invoke()) doReturn true
        verify(condition, times(1)).invoke()
        callbacks.call { onTestAction() }
        verify(condition, times(2)).invoke()
        verify(actions, times(1)).onTestAction()
    }

    @Test
    fun `'callAlways' instances are equal if actions are equal`() {
        val actions = mock<TestActions>()
        val firstInstance = callAlways(actions)
        val secondInstance = callAlways(actions)
        assertThat(firstInstance).isEqualTo(secondInstance)
        assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode())
    }

    @Test
    fun `'callAlways' instances are not equal if actions are not equal`() {
        val firstActions = mock<TestActions>()
        val secondActions = mock<TestActions>()
        val firstInstance = callAlways(firstActions)
        val secondInstance = callAlways(secondActions)
        assertThat(firstInstance).isNotEqualTo(secondInstance)
    }

    @Test
    fun `'callWhen' instances are equal if conditions and actions are equal`() {
        val condition = { true }
        val actions = mock<TestActions>()
        val firstInstance = callWhen(condition, actions)
        val secondInstance = callWhen(condition, actions)
        assertThat(firstInstance).isEqualTo(secondInstance)
        assertThat(firstInstance.hashCode()).isEqualTo(secondInstance.hashCode())
    }

    @Test
    fun `'callWhen' instances are not equal if conditions are not equal`() {
        val firstCondition = { true }
        val secondCondition = { true }
        val actions = mock<TestActions>()
        val firstInstance = callWhen(firstCondition, actions)
        val secondInstance = callWhen(secondCondition, actions)
        assertThat(firstInstance).isNotEqualTo(secondInstance)
    }

    @Test
    fun `'callWhen' instances are not equal if actions are not equal`() {
        val condition = { true }
        val firstActions = mock<TestActions>()
        val secondActions = mock<TestActions>()
        val firstInstance = callWhen(condition, firstActions)
        val secondInstance = callWhen(condition, secondActions)
        assertThat(firstInstance).isNotEqualTo(secondInstance)
    }

    @Test
    fun `'callNever' instances are equal`() {
        val firstInstance = callNever<Int>()
        val secondInstance = callNever<String>()
        assertThat(firstInstance).isEqualTo(secondInstance)
    }

    fun interface TestActions {

        fun onTestAction()
    }
}
