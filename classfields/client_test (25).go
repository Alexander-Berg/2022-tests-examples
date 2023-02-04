package sentry_test

import (
	"errors"
	"fmt"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/sentry"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	postfix string
	conf    sentry.Conf
)

func init() {
	test.InitTestEnv()
	conf = sentry.NewConf()
	UUID := uuid.New().String()
	postfix = UUID[:10]
	cleanProjects()
}

func cleanProjects() {

	conf := sentry.NewConf()
	c := sentry.NewClient(test.NewLoggerWithoutTest(), conf)
	projects, err := c.Projects(conf.Organization, conf.Team)
	if err != nil {
		panic(fmt.Errorf("clean: get projects fail %w", err))
	}
	for _, p := range projects {
		if time.Now().Sub(*p.DateCreated) > 24*time.Hour {
			err := c.DeleteProject(conf.Organization, p.Slug)
			if err != nil {
				panic(fmt.Errorf("clean: delete project '%s' fail %w", p.Name, err))
			}
		}
	}
}

// sentry create new project with max length = 30 (update project can set more length)
func projectName(t *testing.T) string {
	name := t.Name()
	if len(name) > 20 {
		name = name[:20]
	}
	// sentry use lower case
	return strings.ToLower(name + postfix)
}

func emptyProject(t *testing.T, client sentry.IClient) *sentry.ProjectResponse {
	conf := sentry.NewConf()
	project, err := client.CreateProject(conf.Organization, conf.Team, projectName(t))
	require.NoError(t, err)
	return project
}

func newClient(t *testing.T) sentry.IClient {
	test.InitTestEnv()
	return sentry.NewClient(test.NewLogger(t), sentry.NewConf())
}

func deleteProject(t *testing.T, client sentry.IClient) {
	conf := sentry.NewConf()
	name := projectName(t)
	test.Wait(t, func() error {
		_, err := client.Project(conf.Organization, name)
		return err
	})
	require.NoError(t, client.DeleteProject(conf.Organization, name))
}

func TestClient_GetProject(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)

	project, err := c.Project(conf.Organization, p.Slug)
	require.NoError(t, err)
	assert.Equal(t, "active", project.Status)
}

func TestClient_GetProject_NotFound(t *testing.T) {
	c := newClient(t)

	_, err := c.Project("verticals", projectName(t))
	assert.True(t, errors.Is(err, common.ErrNotFound))
}

func TestClient_Keys(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)

	result, err := c.Keys(conf.Organization, p.Name)
	require.NoError(t, err)
	assert.Len(t, result, 1)
	assert.NotEmpty(t, result[0].Secret)
}

func TestClient_CreateProject(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)

	assert.Equal(t, projectName(t), p.Slug)
	assert.Equal(t, projectName(t), p.Name)
}

func TestClient_CreateKey(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)

	key, err := c.CreateKey("verticals", p.Name, "test_key")
	require.NoError(t, err)
	assert.Equal(t, "test_key", key.Name)
	assert.True(t, key.IsActive)
	require.Nil(t, key.RateLimit)
}

func TestClient_UpdateLimit(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)

	keys, err := c.Keys(conf.Organization, p.Slug)
	require.NoError(t, err)
	assert.Len(t, keys, 1)
	k := keys[0]
	require.Nil(t, k.RateLimit)

	require.NoError(t, c.UpdateKey(k.ID, conf.Organization, p.Name, k.Name,
		&sentry.Limit{
			Count:  100,
			Window: 60,
		}))

	keys, err = c.Keys(conf.Organization, p.Slug)
	require.NoError(t, err)
	assert.Len(t, keys, 1)
	updatedK := keys[0]
	require.NotNil(t, updatedK.RateLimit)
	assert.Equal(t, 100, updatedK.RateLimit.Count)
	assert.Equal(t, 60, updatedK.RateLimit.Window)
}
