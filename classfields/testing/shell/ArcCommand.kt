package com.yandex.mobile.realty.testing.shell

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ArcCommand internal constructor(private val shell: ShellScript) {

    private val gson = Gson()
    private val diffType = object : TypeToken<List<DiffFiles>>() {}.type
    private val branchType = object : TypeToken<List<Branch>>() {}.type

    fun getCurrentBranchName(): String =
        getBranch(listOf("--points-at", HEAD)).firstOrNull { it.current }?.name ?: HEAD

    fun mergeBase(branch1: String, branch2: String): String = arcCommand(listOf("merge-base", branch1, branch2))

    fun diffFiles(branch1: String, branch2: String): List<DiffFiles> {
        val diff = arcCommand(listOf("diff", branch1, branch2, "--name-status", JSON))
        return gson.fromJson(diff, diffType)
    }

    fun diff(branch1: String, branch2: String, path: String): String =
        arcCommand(listOf("diff", branch1, branch2, path))

    private fun getBranch(params: List<String>): List<Branch> {
        val branches = arcCommand(listOf("branch") + params + listOf(JSON))
        return gson.fromJson(branches, branchType)
    }

    private fun arcCommand(arguments: List<String>): String = shell.command("arc", arguments)

    companion object {
        const val TRUNK_BRANCH = "trunk"
        const val HEAD = "HEAD"

        private const val JSON = "--json"
    }
}

data class Branch(
    val local: Boolean,
    val name: String,
    val current: Boolean,
)

data class DiffFiles(
    val names: List<DiffFile>
) {

    data class DiffFile(
        val status: String,
        val path: String,
    ) {

        fun isNewFile() = status == NEW_FILE

        fun isModified() = status == MODIFIED

        companion object {
            private const val NEW_FILE = "new file"
            private const val MODIFIED = "modified"
        }
    }
}
