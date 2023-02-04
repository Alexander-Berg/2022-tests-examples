package feature_flags

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_GetValue(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))
	l, err := s.Get(TestMytOff.String())
	require.NoError(t, err)
	assert.False(t, l.Value)

	m := []*FeatureFlag{}
	require.NoError(t, s.base.GetAll("", &m, ""))

	assert.Len(t, m, 0)
}

func TestStorage_SetValue(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	flag := TestMytOff.String()

	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))

	require.NoError(t, s.Set(&FeatureFlag{
		Flag:   flag,
		Value:  true,
		Reason: "учения",
	}))

	m := []*FeatureFlag{}
	require.NoError(t, s.base.GetAll("", &m, ""))
	assert.Len(t, m, 1)

	l, err := s.Get(flag)
	require.NoError(t, err)
	assert.True(t, l.Value)
	assert.Equal(t, "учения", l.Reason)

	require.NoError(t, s.base.GetAll("", &m, ""))
	assert.Len(t, m, 1)

	require.NoError(t, s.Set(&FeatureFlag{
		Flag:   flag,
		Value:  false,
		Reason: "",
	}))

	require.NoError(t, s.base.GetAll("", &m, ""))
	assert.Len(t, m, 1)

	l, err = s.Get(flag)
	require.NoError(t, err)
	assert.False(t, l.Value)
	assert.Equal(t, "", l.Reason)

	require.NoError(t, s.base.GetAll("", &m, ""))

	assert.Len(t, m, 1)

	require.NoError(t, s.Set(&FeatureFlag{
		Flag:   ProdSasOff.String(),
		Value:  false,
		Reason: "test",
	}))
	require.NoError(t, s.base.GetAll("", &m, ""))

	assert.Len(t, m, 2)
}

func TestStorage_GetFlags(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	s := NewStorage(test_db.NewDb(t), test.NewLogger(t))
	flags, err := s.GetFlags([]string{TestMytOff.String(), TestSasOff.String()})
	require.NoError(t, err)
	require.Len(t, flags, 2)
	assert.False(t, flags[0].Value)
	assert.False(t, flags[1].Value)

	m := []*FeatureFlag{}
	require.NoError(t, s.base.GetAll("", &m, ""))

	assert.Len(t, m, 0)
}
