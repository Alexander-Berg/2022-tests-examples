package issue_link

import (
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestService_Issue(t *testing.T) {
	var (
		dID = int64(101)
		key = "VOID-103"
	)
	test.RunUp(t)
	test.Down(t)

	s := newService(t)
	initIssue, err := s.tracker.GetIssueByKey(key)
	require.NoError(t, err)
	require.NotNil(t, initIssue)
	require.NoError(t, s.LinkByIssues(dID, []*issue.Issue{initIssue}))

	issues, err := s.Issues(dID)
	require.NoError(t, err)
	assert.Equal(t, len(issues), 1)
	resultIssue := issues[0]
	require.NotNil(t, resultIssue)
	assert.Equal(t, resultIssue.Key, initIssue.Key)
	assert.Equal(t, resultIssue.ID, initIssue.ID)
}

func TestService_Key(t *testing.T) {
	var (
		dID = int64(100)
		key = "VOID-102"
	)
	test.RunUp(t)
	test.Down(t)
	s := newService(t)

	issues, err := s.LinkByKeys(dID, []string{key})
	require.NoError(t, err)
	assert.Equal(t, len(issues), 1)
	i := issues[0]
	require.NotNil(t, i)
	assert.Equal(t, key, i.Key)

	keys, err := s.IssueKeys(dID)
	require.NoError(t, err)
	assert.Equal(t, len(issues), 1)
	resultKey := keys[0]
	assert.Equal(t, key, resultKey)
}

func TestService_Copy(t *testing.T) {

	var (
		dID1 = int64(103)
		dID2 = int64(104)
		key1 = "VOID-102"
		key2 = "VOID-103"
	)
	test.RunUp(t)
	test.Down(t)
	s := newService(t)

	_, err := s.LinkByKeys(dID1, []string{key1, key2})
	require.NoError(t, err)
	issues2, err := s.Copy(dID2, dID1)
	assert.Equal(t, 2, len(issues2))

	resultIssues1, err := s.Issues(dID1)
	require.NoError(t, err)
	assert.Equal(t, 2, len(resultIssues1))

	resultIssues2, err := s.Issues(dID2)
	require.NoError(t, err)
	assert.Equal(t, 2, len(resultIssues2))
}

func newService(t *testing.T) *Service {

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	deployDataSvc := data.NewService(db, log)
	tracker := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	return NewService(db, log, tracker)
}
