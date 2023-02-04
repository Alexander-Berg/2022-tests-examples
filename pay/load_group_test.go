package actions

import (
	"context"
	"testing"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/payplatform/fes/fes/pkg/storage/db"
)

type LoadGroupActionTestSuite struct {
	ActionTestSuite
}

func TestLoadGroupActionTestSuite(t *testing.T) {
	t.Parallel()
	suite.Run(t, new(LoadGroupActionTestSuite))
}

func (s *LoadGroupActionTestSuite) TestCreateLoadGroupAction() {
	ctx := context.Background()

	createdLoadGroup, err := CreateLoadGroupAction(ctx, s.repo,
		btesting.RandS(10),
		btesting.RandS(12),
		btesting.RandS(5),
	)
	s.Require().NoError(err)

	id := createdLoadGroup.ID
	gotLoadGroup, err := s.repo.Storage.LoadGroup.Get(ctx, id)
	s.Require().NoError(err)

	s.Assert().Equal(createdLoadGroup, gotLoadGroup)

	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, createdLoadGroup.QueueURL))
	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, createdLoadGroup.DeadLetterQueueURL))
}

func (s *LoadGroupActionTestSuite) TestGetLoadGroupAction() {
	ctx := context.Background()

	createdLoadGroup, err := s.repo.Storage.LoadGroup.Create(ctx,
		btesting.RandS(10),
		btesting.RandS(12),
		btesting.RandS(5),
		btesting.RandS(20),
		btesting.RandS(20),
	)
	s.Require().NoError(err)

	gotLoadGroup, err := GetLoadGroupAction(ctx, s.repo, createdLoadGroup.ID)
	s.Require().NoError(err)

	s.Assert().Equal(createdLoadGroup, gotLoadGroup)
}

func (s *LoadGroupActionTestSuite) TestCreateExistingNameLoadGroupAction() {
	ctx := context.Background()

	name := btesting.RandS(10)

	lg, err := CreateLoadGroupAction(ctx, s.repo,
		name,
		btesting.RandS(10),
		btesting.RandS(10),
	)
	s.Require().NoError(err)

	_, err = CreateLoadGroupAction(ctx, s.repo,
		name,
		btesting.RandS(10),
		btesting.RandS(10),
	)

	s.Assert().ErrorIs(err, db.ErrLoadGroupAlreadyExists)

	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
}

func (s *LoadGroupActionTestSuite) TestCreateExistingINNGroupLoadGroupAction() {
	ctx := context.Background()

	inn := btesting.RandS(10)
	group := btesting.RandS(10)

	lg, err := CreateLoadGroupAction(ctx, s.repo,
		btesting.RandS(10),
		inn,
		group,
	)
	s.Require().NoError(err)

	_, err = CreateLoadGroupAction(ctx, s.repo,
		btesting.RandS(10),
		inn,
		group,
	)

	s.Assert().ErrorIs(err, db.ErrLoadGroupAlreadyExists)

	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.QueueURL))
	s.Assert().NoError(s.repo.SQS.DeleteQueue(ctx, lg.DeadLetterQueueURL))
}

func (s *LoadGroupActionTestSuite) TestGetNotExistingLoadGroupAction() {
	ctx := context.Background()

	_, err := GetLoadGroupAction(ctx, s.repo, btesting.RandUN64())
	s.Assert().ErrorIs(err, db.ErrNotFound)
}
