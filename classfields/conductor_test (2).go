package parsing

import (
	"testing"
)

func checkGetPullRequestOptionsFromConductorCommentResult(comment string, expectedResult Comment, t *testing.T) {
	result, err := NewComment(comment)

	if err != nil {
		t.Errorf(
			"Failed to parse comment '%s', expected: %+v, got error",
			comment,
			expectedResult,
		)
		return
	}

	if result != expectedResult {
		t.Errorf(
			"Failed to parse comment '%s', expected: %+v, got: %+v",
			comment,
			expectedResult,
			result,
		)
		return
	}
}

func TestGetPullRequestOptionsFromConductorComment(t *testing.T) {
	comment0 := "VertisHub create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release"
	expectedResult0 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_12.09.2018_forms",
		BaseBranch:     "master",
		Labels:         "release",
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment0, expectedResult0, t)

	comment1 := "* Build #213 (release_12.09.2018_forms:89d72992fc55b720b77c260061c313659852de53) by natix\n* AUTORUFRONT-12107\n* VertisHub create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release"
	expectedResult1 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_12.09.2018_forms",
		BaseBranch:     "master",
		Labels:         "release",
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment1, expectedResult1, t)

	comment2 := "* Build #213 (release_12.09.2018_forms:89d72992fc55b720b77c260061c313659852de53) by natix\n* AUTORUFRONT-12107\nVertisHub create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release"
	expectedResult2 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_12.09.2018_forms",
		BaseBranch:     "master",
		Labels:         "release",
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment2, expectedResult2, t)

	comment3 := "* Build #455\n(release_r108_parts:7c072fa8f4bf86f2ccd5fdecaeff2ffe43137c11) by\ncaulfield\nVertisHub create_pr=autoru-frontend:release_r108_parts:master add_labels=release\n\nhttps://st.yandex-team.ru/AUTOPARTS-6510"
	expectedResult3 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_r108_parts",
		BaseBranch:     "master",
		Labels:         "release",
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment3, expectedResult3, t)

	comment4 := "VertisHub create_pr=autoru-frontend:release_12.09.2018_forms:master add_branch_label"
	expectedResult4 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_12.09.2018_forms",
		BaseBranch:     "master",
		Labels:         "",
		AddBranchLabel: true,
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment4, expectedResult4, t)

	comment5 := "VertisHub create_pr=autoru-frontend:release_12.09.2018_forms:master add_labels=release add_branch_label"
	expectedResult5 := Comment{
		RepositoryName: "autoru-frontend",
		HeadBranch:     "release_12.09.2018_forms",
		BaseBranch:     "master",
		Labels:         "release",
		AddBranchLabel: true,
	}

	checkGetPullRequestOptionsFromConductorCommentResult(comment5, expectedResult5, t)
}
