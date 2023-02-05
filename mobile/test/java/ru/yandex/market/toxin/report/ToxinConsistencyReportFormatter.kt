package ru.yandex.market.toxin.report

import toxin.issue.DefinitionNullabilityIssue
import toxin.issue.FailedObjectCreationIssue
import toxin.issue.IllegalDefinitionOverrideIssue
import toxin.issue.WrongAccessorMethodIssue
import toxin.issue.MissingCollectionFactoryIssue
import toxin.issue.MissingObjectFactoryIssue
import toxin.issue.ScopeInitializationIssue
import toxin.issue.ToxinIssue
import toxin.issue.UncheckedToxinIssue
import toxin.tools.verifiers.ConsistencyVerifier

class ToxinConsistencyReportFormatter {

    fun format(errors: List<ConsistencyVerifier.VerificationError>): String {
        val clusters = clusterizeErrors(errors)
        return buildString {
            for ((_, cluster) in clusters) {
                val error = cluster.first()
                val item = formatReportItem(error)
                val indent = "\t"
                val detailsWithIndent = item.details.split(System.lineSeparator())
                    .filter { line -> line.isNotEmpty() }
                    .joinToString(
                        prefix = indent,
                        separator = "${System.lineSeparator()}$indent"
                    )
                appendLine()
                appendLine(" ❌ [${item.message}] (${cluster.size} errors)")
                appendLine(detailsWithIndent)
            }
        }
    }

    private fun clusterizeErrors(
        errors: List<ConsistencyVerifier.VerificationError>
    ): Map<String, List<ConsistencyVerifier.VerificationError>> {
        val groups = mutableMapOf<String, MutableList<ConsistencyVerifier.VerificationError>>()
        for (error in errors) {
            val issueId = when (error) {
                is ConsistencyVerifier.ComponentInstantiationFailed -> {
                    "ComponentFailedIssue#${error.componentClass.canonicalName}"
                }
                is ConsistencyVerifier.GetterVerificationFailed -> {
                    val rootCause = findRootCause(error.issue)
                    "GetterVerificationFailed#${rootCause.issueId}"
                }
                is ConsistencyVerifier.LazyResolutionFailed -> {
                    val rootCause = findRootCause(error.issue)
                    "LazyResolutionFailed#${rootCause.issueId}"
                }
                is ConsistencyVerifier.ProviderResolutionFailed -> {
                    val rootCause = findRootCause(error.issue)
                    "ProviderResolutionFailed#${rootCause.issueId}"
                }
            }
            groups.getOrPut(issueId) { mutableListOf() }.add(error)
        }
        return groups
    }

    private fun formatReportItem(error: ConsistencyVerifier.VerificationError): ReportItem {
        return when (error) {
            is ConsistencyVerifier.ComponentInstantiationFailed -> {
                ReportItem(
                    message = error.message,
                    details = error.exception.stackTraceToString()
                )
            }
            is ConsistencyVerifier.GetterVerificationFailed -> {
                val rootCause = findRootCause(error.issue)
                ReportItem(
                    message = rootCause.message,
                    details = rootCause.exception?.stackTraceToString() ?: "<no details>"
                )
            }
            is ConsistencyVerifier.LazyResolutionFailed -> {
                val rootCause = findRootCause(error.issue)
                ReportItem(
                    message = rootCause.message,
                    details = rootCause.exception?.stackTraceToString() ?: "<no details>"
                )
            }
            is ConsistencyVerifier.ProviderResolutionFailed -> {
                val rootCause = findRootCause(error.issue)
                ReportItem(
                    message = rootCause.message,
                    details = rootCause.exception?.stackTraceToString() ?: "<no details>"
                )
            }
        }
    }

    private fun findRootCause(issue: ToxinIssue): ToxinIssue {
        return when (issue) {
            is ScopeInitializationIssue -> issue
            is MissingObjectFactoryIssue -> issue
            is MissingCollectionFactoryIssue -> issue
            is IllegalDefinitionOverrideIssue -> issue
            is DefinitionNullabilityIssue -> issue
            is WrongAccessorMethodIssue -> issue
            is FailedObjectCreationIssue -> findRootCause(issue.cause)
            is UncheckedToxinIssue -> issue
        }
    }

    private data class ReportItem(
        val message: String,
        val details: String
    )
}
