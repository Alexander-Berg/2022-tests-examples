package github

import (
	"context"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func TestGetParentCommitMessages(t *testing.T) {
	parent := "c7894657a1395a2accb56b6866ab16384be89504"
	commitMap := map[string]Commit{
		"691caeaa9713c3a311aa17706d014712baa84587": {
			"4911444477c69c48d44d2117962a1d2685e58ca9",
			"1",
		},
		"94b6c68bfa0fd11b0a99e539326f3b411560de4f": {
			"691caeaa9713c3a311aa17706d014712baa84587",
			"2",
		},
		"b3c6429e4de866ffd5071af427b789cc6120eae4": {
			"4911444477c69c48d44d2117962a1d2685e58ca9",
			"3",
		},
		"05cecd4c7c8b5b26d825a5b7f9a94ebd7993baae": {
			"b3c6429e4de866ffd5071af427b789cc6120eae4",
			"4",
		},
		"c7894657a1395a2accb56b6866ab16384be89504": {
			"94b6c68bfa0fd11b0a99e539326f3b411560de4f",
			"5",
		},
	}

	messages := GetParentCommitMessages(commitMap, parent)
	assert.ElementsMatch(t, messages, []string{"5", "2", "1"})
}

func TestGetPullRequestCommitsMessages(t *testing.T) {
	Init(os.Getenv("GITHUB_API_TOKEN"), "")
	messages, err := GetPullRequestCommitsMessages(context.Background(), "YandexClassifieds", "spooner_test", 6)
	if err != nil {
		panic(err)
	}
	assert.ElementsMatch(t, messages, []string{"5", "2", "1", "t"})
}
