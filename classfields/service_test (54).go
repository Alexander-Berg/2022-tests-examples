package feature_flags

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestGet(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	pMock := NewProducerMock()
	s := NewService(test_db.NewDb(t), pMock, test.NewLogger(t))
	f := TestSasOff.String()
	flag, err := s.Get(f)
	require.NoError(t, err)
	assert.False(t, flag.Value)
	assert.Empty(t, flag.Reason)

	require.NoError(t, s.Set(&FeatureFlag{
		Flag:   f,
		Reason: "r",
	}))

	flag, err = s.Get(f)
	require.NoError(t, err)
	assert.True(t, flag.Value)
	assert.Equal(t, "r", flag.Reason)

	require.NoError(t, s.Clear(&FeatureFlag{
		Flag: f,
	}))

	flag, err = s.Get(f)
	require.NoError(t, err)
	assert.False(t, flag.Value)
	assert.Empty(t, flag.Reason)

}

func TestSetClear(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	pMock := NewProducerMock()
	s := NewService(test_db.NewDb(t), pMock, test.NewLogger(t))
	f := TestSasOff.String()

	require.NoError(t, s.Set(&FeatureFlag{
		Flag:   f,
		Reason: "test",
	}))
	assertValue(t, pMock.Read(t), f, true)

	flag, err := s.Get(f)
	require.NoError(t, err)
	assert.True(t, flag.Value)
	assert.Equal(t, "test", flag.Reason)

	require.NoError(t, s.Clear(&FeatureFlag{
		Flag: f,
	}))
	assertValue(t, pMock.Read(t), f, false)

	flag, err = s.Get(f)
	require.NoError(t, err)
	assert.False(t, flag.Value)
	assert.Empty(t, flag.Reason)
	assert.Empty(t, pMock.Msg)
}

func assertValue(t *testing.T, msg *mq.Message, name string, want bool) {
	flag := &flags.FeatureFlag{}
	err := proto.Unmarshal(msg.Payload, flag)
	require.NoError(t, err)
	assert.Equal(t, name, flag.Name)
	assert.Equal(t, want, flag.Value)
}

func TestSetClearMany(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	pMock := NewProducerMock()
	s := NewService(test_db.NewDb(t), pMock, test.NewLogger(t))
	flags := []string{TestSasOff.String(), TestMytOff.String()}

	require.NoError(t, s.SetFlags([]*FeatureFlag{{
		Flag:   flags[0],
		Reason: "test",
	}, {
		Flag:   flags[1],
		Reason: "test",
	},
	}))
	assertValue(t, pMock.Read(t), flags[0], true)
	assertValue(t, pMock.Read(t), flags[1], true)

	featureFlags, err := s.GetActualFlags(flags)
	require.NoError(t, err)
	assert.True(t, featureFlags[0].Value)
	assert.Equal(t, "test", featureFlags[0].Reason)
	assert.True(t, featureFlags[1].Value)
	assert.Equal(t, "test", featureFlags[1].Reason)

	require.NoError(t, s.ClearFlags([]*FeatureFlag{{
		Flag: flags[0],
	}, {
		Flag: flags[1],
	},
	}))
	assertValue(t, pMock.Read(t), flags[0], false)
	assertValue(t, pMock.Read(t), flags[1], false)

	featureFlags, err = s.GetFlags(flags)
	require.NoError(t, err)
	assert.False(t, featureFlags[0].Value)
	assert.Empty(t, featureFlags[0].Reason)
	assert.False(t, featureFlags[1].Value)
	assert.Empty(t, featureFlags[1].Reason)
	assert.Empty(t, pMock.Msg)
}

// TODO use common test mock
type ProducerMock struct {
	Msg chan *mq.Message
}

func NewProducerMock() *ProducerMock {
	return &ProducerMock{
		Msg: make(chan *mq.Message, 10),
	}
}

func (p *ProducerMock) Push(msg *mq.Message) error {
	select {
	case p.Msg <- msg:
	default:
		panic("chan overflow")
	}
	return nil
}

func (p *ProducerMock) Read(t *testing.T) *mq.Message {
	select {
	case msg := <-p.Msg:
		return msg
	case <-time.NewTimer(time.Second).C:
		require.Fail(t, "timeout")
	}
	return nil
}
