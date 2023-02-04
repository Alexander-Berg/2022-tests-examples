package auth

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
)

func TestAuthToken_Get(t *testing.T) {
	test.InitTestEnv()
	var err error
	db := test_db.NewSeparatedDb(t)
	s := NewStorage(db, test.NewLogger(t))

	token, err := s.NewToken("s1", nil)
	assert.NoError(t, err)

	result, err := s.Get(token)
	assert.NoError(t, err)
	assert.Equal(t, token, result.AccessToken)
}
