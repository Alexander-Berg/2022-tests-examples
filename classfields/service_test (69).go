package sentry_test

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/sentry"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestGenerate(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	log := test.NewLogger(t)
	s := sentry.NewService(log, conf, c)
	name := projectName(t)

	_, err := c.Project(conf.Organization, name)
	require.True(t, errors.Is(err, common.ErrNotFound))
	p, err := s.CreateProject(name)
	require.NoError(t, err)
	assert.Equal(t, name, p.Name)
	assert.Equal(t, name, p.Slug)
	assert.NotEmpty(t, p.ProdDSN)
	assert.NotEmpty(t, p.TestDSN)
	assert.NotEqual(t, p.TestDSN, p.ProdDSN)
}

func TestGenerateExistNewProject(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	log := test.NewLogger(t)
	s := sentry.NewService(log, conf, c)
	name := projectName(t)

	_, err := c.Project(conf.Organization, name)
	require.True(t, errors.Is(err, common.ErrNotFound))
	p1, err := s.CreateProject(name)
	require.NoError(t, err)
	p2, err := s.CreateProject(name)
	require.NoError(t, err)

	assert.NotEmpty(t, p1.ProdDSN)
	assert.NotEmpty(t, p1.TestDSN)
	assert.NotEqual(t, p1.TestDSN, p1.ProdDSN)

	assert.Equal(t, p1.Name, p2.Name)
	assert.Equal(t, p1.Slug, p2.Slug)
	assert.Equal(t, p1.TestDSN, p2.TestDSN)
	assert.Equal(t, p1.ProdDSN, p2.ProdDSN)
}

func TestGenerateExistOldProject(t *testing.T) {
	c := newClient(t)
	defer deleteProject(t, c)
	p := emptyProject(t, c)
	log := test.NewLogger(t)
	s := sentry.NewService(log, conf, c)

	keys, err := c.Keys(conf.Organization, p.Name)
	require.NoError(t, err)
	assert.Len(t, keys, 1)
	assert.Equal(t, "Default", keys[0].Name)

	newProject, err := s.CreateProject(p.Name)
	require.NoError(t, err)
	assert.Equal(t, p.Name, newProject.Name)
	assert.Equal(t, p.Slug, newProject.Slug)
	assert.NotEmpty(t, newProject.ProdDSN)
	assert.NotEmpty(t, newProject.TestDSN)
	assert.NotEqual(t, newProject.TestDSN, newProject.ProdDSN)
}

func TestCreateProjectExistOldRenamed(t *testing.T) {
	c := &mock.Client{}
	c.On("Project", conf.Organization, t.Name()).Return(&sentry.ProjectResponse{Name: t.Name(), Slug: "other name"}, nil).Once()
	c.On("CreateProject", conf.Organization, conf.Team, t.Name()).Return(&sentry.ProjectResponse{Name: t.Name(), Slug: t.Name()}, nil).Once()
	c.On("Key", mock2.Anything, mock2.Anything, mock2.Anything).Return(&sentry.Key{}, nil)
	c.On("UpdateKey", mock2.Anything, mock2.Anything, mock2.Anything, mock2.Anything, mock2.Anything).Return(nil)
	log := test.NewLogger(t)
	s := sentry.NewService(log, conf, c)

	_, err := s.CreateProject(t.Name())
	require.NoError(t, err)

	c.AssertExpectations(t)
}
