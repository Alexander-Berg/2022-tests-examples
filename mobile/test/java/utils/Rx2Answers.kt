package utils

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

object Rx2Answers {

    val Never = Answer { invocation ->
        @Suppress("USELESS_CAST")
        when {
            invocation.returns(Observable::class.java) -> Observable.never<Any>()
            invocation.returns(Single::class.java) -> Single.never<Any>()
            invocation.returns(Completable::class.java) -> Completable.never()
            else -> null as Any?
        }
    }

    private fun InvocationOnMock.returns(clazz: Class<*>): Boolean {
        return method.returnType.isAssignableFrom(clazz)
    }
}
