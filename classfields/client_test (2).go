package arcanum

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
)

const (
	testPRId    int64 = 2651970
	testBranch        = "test-branch"
	testSummary       = "test summary"
	testIssue         = "TEST-123"
)

func TestClient_GetPullRequestStatus(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	status, err := svc.GetPullRequestStatus(testPRId)
	require.NoError(t, err)
	require.Equal(t, StatusDiscarded, status)
}

// we can't create PR in real Arcanum, because of many things, so we use mocks
func TestClient_CreatePullRequest(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	httpmock.Activate()
	defer httpmock.Deactivate()

	httpmock.RegisterResponder(http.MethodPost, fmt.Sprintf(createPRUrlF, c.Str("ARCANUM_URL")),
		func(req *http.Request) (*http.Response, error) {
			require.Equal(t, "OAuth "+c.Str("ARCANUM_TOKEN"), req.Header.Get("Authorization"))

			body, err := ioutil.ReadAll(req.Body)
			require.NoError(t, err)

			var pr PullRequest
			require.NoError(t, json.Unmarshal(body, &pr))
			require.Equal(t, testBranch, pr.VCS.From)
			require.Equal(t, "trunk", pr.VCS.To)
			require.Equal(t, testSummary, pr.Summary)
			require.Equal(t, AutoMergeDisabled, pr.AutoMerge)

			resp, err := httpmock.NewJsonResponse(200, PullRequestResponse{
				Data: PullRequest{Id: testPRId},
			})
			require.NoError(t, err)
			return resp, nil
		})

	pr, err := svc.CreatePullRequest(testBranch, testSummary)
	require.NoError(t, err)
	require.Equal(t, testPRId, pr)
}

// we can't create PR in real Arcanum, because of many things, so we use mocks
func TestClient_CommentPullRequest(t *testing.T) {
	c := viper.NewTestConf()
	logger := logrus.NewLogger()

	svc := New(c, logger)

	httpmock.Activate()
	defer httpmock.Deactivate()

	httpmock.RegisterResponder(http.MethodPost, fmt.Sprintf(commentPRUrlF, c.Str("ARCANUM_URL"), testPRId),
		func(req *http.Request) (*http.Response, error) {
			require.Equal(t, "OAuth "+c.Str("ARCANUM_TOKEN"), req.Header.Get("Authorization"))

			body, err := ioutil.ReadAll(req.Body)
			require.NoError(t, err)

			var comment Comment
			require.NoError(t, json.Unmarshal(body, &comment))
			require.Equal(t, fmt.Sprintf("Issue created: %s", testIssue), comment.Content)
			require.False(t, comment.Issue)

			resp, err := httpmock.NewJsonResponse(200, PullRequestResponse{
				Data: PullRequest{Id: testPRId},
			})
			require.NoError(t, err)
			return resp, nil
		})

	err := svc.CommentPullRequest(testPRId, testIssue)
	require.NoError(t, err)
}
