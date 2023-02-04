import scala.sys.process._

implicit class CommandOutputOps(private val output: String) extends AnyVal {

  def trimmedLines: List[String] = output.lines.map(_.trim).toList
}

def branches(branch: String, in: Boolean) = {
  val mergedParam = if (in) "--merged" else "--no-merged"
  List("git", "branch", "-r", mergedParam, branch).!!.trimmedLines.toSet
}

def branchesIn(branch: String) = branches(branch, in = true) - branch

def branchesNotIn(branch: String) = branches(branch, in = false)

val notMerged = branchesNotIn("origin/master")

def notMergedBranchesIn(branch: String) =
  branchesIn(branch) & notMerged

def chooseBranches(branches: Set[String]) = {
  val indexedBranches = branches.toList.zipWithIndex.map(_.swap)
  val mappedBranches = indexedBranches.toMap
  println("Choose branches to stay in testing:")
  indexedBranches.foreach { case (index, branch) =>
    println(s"[$index] $branch")
  }
  println(
    "Put all needed numbers separated by space in one line (e.g.: 0 3 5):"
  )
  val chosenIndexes =
    scala.io.StdIn.readLine().split(" ").map(_.toInt).toSet
  mappedBranches.filterKeys(chosenIndexes.contains).values.toList
}

def merge(branch: String): Unit = {
  s"git merge --no-ff $branch".!!
  println(s"$branch merged")
}

"git fetch".!!
val testBranches = notMergedBranchesIn("origin/testing")
val newTestBranches = chooseBranches(testBranches)
"git checkout testing".!!
"git reset --hard origin/master".!!
newTestBranches.foreach(merge)
"git push -f".!!
println("testing force-pushed successfully")
