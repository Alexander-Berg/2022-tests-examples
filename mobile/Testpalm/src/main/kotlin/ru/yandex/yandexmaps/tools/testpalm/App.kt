package ru.yandex.yandexmaps.tools.testpalm

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.optional
import ru.yandex.yandexmaps.tools.SecretsRepository
import ru.yandex.yandexmaps.tools.testpalm.impl.Config
import ru.yandex.yandexmaps.tools.testpalm.impl.ConfigLoader
import ru.yandex.yandexmaps.tools.testpalm.impl.ScenarioRunner
import java.io.File
import java.util.Properties
import kotlin.system.exitProcess

@ExperimentalCli
fun main(args: Array<String>) {
    val parser = ArgParser(programName = "testpalm")

    class Generate(private val config: Config, private val oauth: String) : Subcommand("generate", "Generate testcase template") {
        val id: String by argument(ArgType.String, "Testcase id (number of [projectId]-[number])")
        val replace: Boolean by option(ArgType.Boolean, "force", "f", "replace template with new version if necessary").default(false)

        override fun execute() {
            val runner = ScenarioRunner(config, oauth)
            runner.generateTestcaseTemplate(id, replace)
        }
    }

    class Check(private val config: Config, private val oauth: String) : Subcommand("check", "Check if local testcase is up-to-date") {
        val id: String? by argument(ArgType.String, "Testcase id (number of [projectId]-[number])").optional()

        override fun execute() {
            val runner = ScenarioRunner(config, oauth)
            runner.checkLocalTestcases(id)
        }
    }

    try {
        val config = ConfigLoader.loadConfig()

        val oauth = SecretsRepository().hamsterAutomationOauthToken

        parser.subcommands(Generate(config, oauth), Check(config, oauth))

        parser.parse(args)
    } catch (t: Throwable) {
        t.printStackTrace()
        exitProcess(1)
    }

    exitProcess(0)
}
