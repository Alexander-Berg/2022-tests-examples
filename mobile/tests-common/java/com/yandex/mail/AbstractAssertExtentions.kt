package com.yandex.mail

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Condition

/**
 *  Alias to get rid of kotlin system keyword usage.
 */
fun <A> AbstractAssert<*, out A>.conforms(condition: Condition<A>) = this.`is`(condition)!!
