package com.yandex.mobile.realty.testing.shell

import java.io.File

fun <T> shellRun(workingDirectory: File? = null, script: ShellScript.() -> T): T =
    ShellScript(workingDirectory).script()
