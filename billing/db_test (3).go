package request

import (
	"time"

	"github.com/stretchr/testify/require"
)

func (s *ProcessorTestSuite) TestGetNew() {
	r, err := Create(s.ctx, createRandomRequest())
	require.NoError(s.T(), err)
	require.Equal(s.T(), r.Status, StatusNew)

	rs, tx, err := GetNew(s.ctx, 1000)
	require.NoError(s.T(), err)
	require.Greater(s.T(), len(rs), 0)
	found := false
	for _, req := range rs {
		if req.ID == r.ID {
			found = true
		}
	}
	require.True(s.T(), found)

	rs1, tx1, err1 := GetNew(s.ctx, 10)
	require.NoError(s.T(), err1)
	require.Len(s.T(), rs1, 0)

	require.NoError(s.T(), tx.Rollback())
	require.NoError(s.T(), tx1.Rollback())

	rs2, tx2, err2 := GetNew(s.ctx, 0)
	require.NoError(s.T(), err2)
	require.Len(s.T(), rs2, 0)
	require.NoError(s.T(), tx2.Rollback())
}

func (s *ProcessorTestSuite) TestGetStatusCounts() {
	_, err := Create(s.ctx, createRandomRequest())
	require.NoError(s.T(), err)

	counts, err := GetCountByStatus(s.ctx)
	require.NoError(s.T(), err)

	_, ok := counts[StatusNew]
	require.True(s.T(), ok)
}

func (s *ProcessorTestSuite) TestGetLastCountByStatus() {
	count, err := GetLastCountByStatus(s.ctx, time.Now().UTC())
	require.NoError(s.T(), err)
	require.EqualValues(s.T(), 0, count[StatusError])
	require.EqualValues(s.T(), 0, count[StatusDone])
	require.EqualValues(s.T(), 0, count[StatusNew])
}
