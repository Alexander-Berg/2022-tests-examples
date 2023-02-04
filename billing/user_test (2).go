package actions

import (
	"context"
	"testing"

	"github.com/stretchr/testify/suite"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/template-project/pkg/core/entities"
)

type UserActionTestSuite struct {
	ActionTestSuite
}

func (s *UserActionTestSuite) TestCreateUserAction() {
	var err error
	ctx := context.Background()

	expected := entities.User{
		UID:  uint64(btesting.RandN64()), // Postgres does have an unsigned integer type
		Name: btesting.RandS(100),
	}

	_, err = CreateUserAction(ctx, expected.UID, expected.Name, s.repo.Storage)
	s.Require().NoError(err)

	fromDatabase, err := s.repo.Storage.User.Get(ctx, expected.UID)
	s.Require().NoError(err)

	s.Assert().Equal(expected, *fromDatabase)
}

func (s *UserActionTestSuite) TestCreateMultipleUsersAction() {
	var err error
	ctx := context.Background()

	expected := []entities.User{
		{
			UID:  uint64(btesting.RandN64()),
			Name: btesting.RandS(100),
		},
		{
			UID:  uint64(btesting.RandN64()),
			Name: btesting.RandS(100),
		},
	}

	err = CreateMultipleUsersAction(ctx, expected, s.repo.Storage)
	s.Require().NoError(err)

	for _, user := range expected {
		fromDatabase, err := s.repo.Storage.User.Get(ctx, user.UID)
		s.Require().NoError(err)

		s.Assert().Equal(user, *fromDatabase)
	}
}

func (s *UserActionTestSuite) TestGetMultipleUsersAction() {
	var err error
	ctx := context.Background()

	expected := []entities.User{
		{
			UID:  uint64(btesting.RandN64()),
			Name: btesting.RandS(100),
		},
		{
			UID:  uint64(btesting.RandN64()),
			Name: btesting.RandS(100),
		},
	}
	uids := make([]uint64, 2)
	for i, user := range expected {
		_, err := s.repo.Storage.User.Create(ctx, user)
		s.Require().NoError(err)
		uids[i] = user.UID
	}

	res, err := GetMultipleUsersAction(ctx, uids, s.repo.Storage)
	s.Require().NoError(err)
	s.Assert().Equal(expected, res)
}

func TestUserActionTestSuite(t *testing.T) {
	suite.Run(t, new(UserActionTestSuite))
}
