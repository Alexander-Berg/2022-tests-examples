package api

import (
	"errors"
	"fmt"
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	key = "TEST-30540"
)

func TestTrackerApi_GetQueue(t *testing.T) {
	test.InitTestEnv()
	api := NewApi(NewConf(), test.NewLogger(t))
	t.Run("existing", func(t *testing.T) {
		q, err := api.GetQueue("VOID")
		require.NoError(t, err)
		assert.Equal(t, "VOID", q.Key)
	})
	t.Run("not_found", func(t *testing.T) {
		_, err := api.GetQueue("the-one-that-should-not-exist-ever")
		require.Error(t, err)
		assert.True(t, errors.Is(err, common.ErrNotFound), "should be matched to ErrNotFound, got %T", err)
	})
}

func TestAPI_FindIssues(t *testing.T) {
	test.InitTestEnv()
	api := NewApi(NewConf(), test.NewLogger(t))

	var keys []string
	for i := 1; i < 150; i++ {
		keys = append(keys, fmt.Sprintf("VOID-%d", i))
	}
	// add a duplicate
	keys = append(keys, "VOID-1")

	issues, err := api.FindIssues(fmt.Sprintf("Key: %s", strings.Join(keys, ",")))
	assert.NoError(t, err)
	assert.Equal(t, len(issues), len(keys)-1)
}

func TestGetRemoteLinks(t *testing.T) {
	test.InitTestEnv()
	api := NewApi(NewConf(), test.NewLogger(t))
	clean(t, api)

	expectedRequests := []CreateRemoteLinkRequest{
		{
			Relationship: "relates",
			Key:          test.RandString(5),
			Origin:       "shiva.deployment.1",
		},
		{
			Relationship: "relates",
			Key:          test.RandString(5),
			Origin:       "shiva.deployment.1",
		},
	}

	var actualLinks []*RemoteLinkResponse
	for _, req := range expectedRequests {
		resp, err := api.PostRemoteLink(key, req)
		require.NoError(t, err)
		actualLinks = append(actualLinks, resp)
	}

	_, err := api.PostRemoteLink(key, CreateRemoteLinkRequest{
		Relationship: "relates",
		Key:          test.RandString(5),
		Origin:       "vertis.glue.1",
	})
	require.NoError(t, err)

	fullLinks, err := api.GetRemoteLinks(key, "")
	require.NoError(t, err)
	require.Len(t, fullLinks, 3)

	expectedLinks, err := api.GetRemoteLinks(key, "shiva.deployment.1")
	require.NoError(t, err)
	require.Len(t, expectedLinks, 2)
	require.ElementsMatch(t, expectedLinks, actualLinks)
}

func clean(t *testing.T, api *API) {
	links, err := api.GetRemoteLinks(key, "")
	require.NoError(t, err)

	for _, l := range links {
		err := api.DeleteRemoteLink(key, l.ID)
		require.NoError(t, err)
	}
}
