package ru.auto.testextension

import com.appmattus.kotlinfixture.Context
import com.appmattus.kotlinfixture.config.ConfigurationBuilder
import com.appmattus.kotlinfixture.decorator.constructor.ConstructorStrategy
import com.appmattus.kotlinfixture.decorator.constructor.constructorStrategy
import com.appmattus.kotlinfixture.kotlinFixture
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor

fun kotlinFixtureDefault(configuration: ConfigurationBuilder.() -> Unit = {}) =
    kotlinFixture {
        constructorStrategy(NoSynteticConstructorsStrategy)
        subType<List<*>, ArrayList<*>>()
        configuration()
    }


/**
 * Ignore syntetic constructors like default parameters contructor and serialization constructor
 */
object NoSynteticConstructorsStrategy : ConstructorStrategy {
    override fun constructors(context: Context, obj: KClass<*>): Collection<KFunction<*>> =
        obj.constructors.filter { it.javaConstructor?.isSynthetic == false }.sortedBy { it.parameters.size }
}
