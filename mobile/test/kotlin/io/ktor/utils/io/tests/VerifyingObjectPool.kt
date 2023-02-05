package io.ktor.utils.io.tests

import io.ktor.utils.io.pool.*
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.MultipleFailureException
import org.junit.runners.model.Statement
import java.util.concurrent.ConcurrentHashMap

class VerifyingObjectPool<T : Any>(delegate: ObjectPool<T>) : VerifyingPoolBase<T>(delegate), TestRule {
    override val allocated = ConcurrentHashMap<IdentityWrapper<T>, Boolean>().keySet(true)!!

    override fun apply(base: Statement, description: Description): Statement {
        return object: Statement() {
            override fun evaluate() {
                var failed = false
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    failed = true
                    try {
                        assertEmpty()
                    } catch (emptyFailed: Throwable) {
                        throw MultipleFailureException(listOf(t, emptyFailed))
                    }
                    throw t
                } finally {
                    if (!failed) {
                        assertEmpty()
                    }
                }
            }
        }
    }
}
