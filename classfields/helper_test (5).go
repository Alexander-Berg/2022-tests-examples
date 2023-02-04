package tracker

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestParseIssues(t *testing.T) {
	actual := ParseIssues("VOID-1: Add some new feature")
	expected := []string{"VOID-1"}
	assert.Equal(t, expected, actual)
}

func TestParseIssues_Empty(t *testing.T) {
	actual := ParseIssues("Add some new feature")
	var expected []string
	assert.Equal(t, expected, actual)
}

func TestParseIssuesMarkdownLinks(t *testing.T) {
	actual := ParseIssuesMarkdownLinks(`
some description
[VOID-1](https://st.yandex-team.ru/VOID-1)
[VOID-2](https://st.yandex-team.ru/VOID-2)
`)
	expected := []string{"VOID-1", "VOID-2"}
	assert.ElementsMatch(t, expected, actual)
}

func TestParseIssuesMarkdownLinks_Empty(t *testing.T) {
	actual := ParseIssuesMarkdownLinks("Some description")
	var expected []string
	assert.Equal(t, expected, actual)
}
