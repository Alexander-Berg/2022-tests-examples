package arcanum

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/pkg/arcanum/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestGetPullRequest(t *testing.T) {
	test.InitTestEnv()

	s := newService(t)

	prInfo, err := s.GetPrInfo(2516510)
	require.NoError(t, err)

	expectedInfo := &PrInfo{
		UpdatedAt: time.Date(2022, time.May, 5, 16, 18, 44, 840962000, time.UTC),
		Id:        2516510,
		Title:     "VERTISADMIN-28059: new mdb kafka proto type",
		Author:    "r-vishnevsky",
		Branch:    "users/robot-stark/classifieds/services/2516510/merge_pin",
		Approvers: []string{"r-vishnevsky", "makarychev"},
		Reviewers: []string{"danevge", "makarychev", "r-vishnevsky"},
	}
	require.Equal(t, expectedInfo, prInfo)
}

func TestComment(t *testing.T) {
	test.InitTestEnv()

	s := newService(t)

	err := s.Comment(2458795, "comment test")
	require.NoError(t, err)
}

func TestReplaceReviewers(t *testing.T) {
	test.InitTestEnv()

	s := newService(t)

	err := s.ReplaceReviewers(2498871, nil)
	require.NoError(t, err)
	prInfo, err := s.GetPrInfo(2498871)
	require.NoError(t, err)
	require.Empty(t, prInfo.Reviewers)
}

func TestPutReviewers(t *testing.T) {
	test.InitTestEnv()

	s := newService(t)

	err := s.ReplaceReviewers(2498871, nil)
	require.NoError(t, err)
	prInfo, err := s.GetPrInfo(2498871)
	require.NoError(t, err)
	require.Empty(t, prInfo.Reviewers)

	err = s.PutReviewers(2498871, []string{"robot-vertis-shiva", "niklogvinenko"})
	require.NoError(t, err)
	prInfo, err = s.GetPrInfo(2498871)
	require.NoError(t, err)
	require.Equal(t, []string{"niklogvinenko", "robot-vertis-shiva"}, prInfo.Reviewers)
}

func newService(t *testing.T) *Service {
	c := client.NewClient(client.NewConf(), test.NewLogger(t))

	return NewService(c, test.NewLogger(t))
}
