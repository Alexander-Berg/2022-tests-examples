package delegation

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestDelegationToken_Save(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	s := NewStorage(db, test.NewLogger(t))
	err := s.Save(&Token{
		ServiceName: "dtsave-svc",
		SecretId:    "sec-wtf",
		TvmId:       0,
		TokenId:     "t1-id",
		Token:       "t1",
	})
	require.NoError(t, err)

	err = s.Save(&Token{
		ServiceName: "dtsave-svc",
		SecretId:    "sec-wtf",
		TvmId:       44,
		TokenId:     "t2-id",
		Token:       "t2",
	})
	require.NoError(t, err)

	t1, err := s.GetBySecret("dtsave-svc", "sec-wtf", 0)
	require.NoError(t, err)
	assert.Equal(t, "t1", t1.Token)
	t2, err := s.GetBySecret("dtsave-svc", "sec-wtf", 44)
	require.NoError(t, err)
	assert.Equal(t, "t2", t2.Token)

	err = s.Save(&Token{
		ServiceName: "dtsave-svc",
		SecretId:    "sec-wtf",
		TvmId:       44,
		TokenId:     "t2-id-new",
		Token:       "t2-new",
	})
	require.NoError(t, err)

	t3, err := s.GetBySecret("dtsave-svc", "sec-wtf", 44)
	require.NoError(t, err)
	assert.Equal(t, "t2-new", t3.Token)
}
