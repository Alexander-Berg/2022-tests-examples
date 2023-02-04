package impl

import (
	"context"
	"testing"
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/entities"
)

func (s *ObjectActionTestSuite) TestAddNoObjects() {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	err := s.actions.AddObjects(ctx, nil)
	s.Require().NoError(err)
}

func (s *ObjectActionTestSuite) TestAddObject() {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	err := s.actions.AddObjects(ctx, []entities.Object{
		{
			ID:            "123",
			DataNamespace: "data",
			LastEventTime: time.Now().Add(time.Hour),
		},
		{
			ID:            "1234",
			DataNamespace: "data",
			LastEventTime: time.Now(),
		},
	})
	s.Require().NoError(err)

	ids, err := s.actions.GetObjectIDsByFilter(ctx, sq.And{
		sq.Eq{"data_ns": "data"},
		sq.GtOrEq{"last_event_ts": time.Now().Add(10 * time.Second)},
	}, 1)
	s.Require().NoError(err)
	s.Require().Len(ids, 1)
	s.Assert().Equal("123", ids[0])

	err = s.actions.AddObjects(ctx, []entities.Object{
		{
			ID:            "1234",
			DataNamespace: "data",
			LastEventTime: time.Now().Add(time.Hour),
		},
	})
	s.Require().NoError(err)

	ids, err = s.actions.GetObjectIDsByFilter(ctx, sq.And{
		sq.Eq{"data_ns": "data"},
		sq.GtOrEq{"last_event_ts": time.Now().Add(10 * time.Second)},
	}, 100)
	s.Require().NoError(err)
	s.Require().Len(ids, 2)
}

type ObjectActionTestSuite struct {
	ActionTestSuite
}

func TestObjectActionTestSuite(t *testing.T) {
	suite.Run(t, new(ObjectActionTestSuite))
}
