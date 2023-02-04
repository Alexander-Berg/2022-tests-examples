package tvmauth

import (
	"encoding/base64"
	"fmt"
	"testing"
	"time"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/go-common/tvm/tvmauth/ticket_parser"
	"github.com/YandexClassifieds/go-common/tvm/tvmauth/ticket_parser/rw"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/proto"
)

func TestTvmClient_ServiceTicket(t *testing.T) {
	log := vlogrus.NewLogger()
	api := new(mockApiClient)
	api.On("ServiceTicket", 42, []int{43, 44}, []byte("some-secret")).Return(map[int]string{
		43: "test-ticket-one",
		44: "some-test-ticket",
	}, nil)
	api.On("GetKeys").Return(nil, nil)

	cli := NewClient(
		WithLogger(log),
		WithIssueTicket(42, []int{43, 44}, base64.RawURLEncoding.EncodeToString([]byte("some-secret"))),
		withApiClient(api),
	)

	ticketStr, err := cli.ServiceTicket(42, 44)
	require.NoError(t, err)
	assert.Equal(t, "some-test-ticket", ticketStr)

	_, err = cli.ServiceTicket(42, 585)
	assert.Equal(t, ErrNoTicket, err)
}

func TestTvmClient_CheckServiceTicket(t *testing.T) {
	k1 := rw.GenerateKey(2048)
	k2 := rw.GenerateKey(2048)
	testKeys := map[uint32]*rw.PublicKey{
		123: k1.PublicKey,
		124: k2.PublicKey,
	}
	api := new(mockApiClient)
	api.On("GetKeys").Return(testKeys, nil)
	cli := NewClient(withApiClient(api))

	okTicket := makeTestTicket(k1, 123, time.Now().Add(time.Hour))
	ticketInfo, err := cli.CheckServiceTicket(okTicket)
	require.NoError(t, err)

	_, err = cli.CheckServiceTicket(makeTestTicket(k1, 123, time.Now().Add(-time.Hour)))
	require.Error(t, err)
	assert.Contains(t, err.Error(), "expired")

	assert.Equal(t, 11, ticketInfo.SrcID)
	assert.Equal(t, 222, ticketInfo.DstID)

	_, err = cli.CheckServiceTicket(makeTestTicket(k2, 5654, time.Now().Add(time.Hour))) // non-existent key-id
	assert.Error(t, err)
	_, err = cli.CheckServiceTicket("3:serv:gibberish") // wrong ticket
	assert.Error(t, err)
}

func makeTestTicket(key *rw.PrivateKey, keyID uint32, expire time.Time) string {
	ticket := &ticket_parser.Ticket{
		KeyId:          keyID,
		ExpirationTime: expire.Unix(),
		Service: &ticket_parser.ServiceTicket{
			SrcClientId: 11,
			DstClientId: 222,
		},
	}
	data, _ := proto.Marshal(ticket)
	signPart := fmt.Sprintf("3:serv:%s:", base64.RawURLEncoding.EncodeToString(data))
	sign := key.Sign([]byte(signPart))
	return signPart + base64.RawURLEncoding.EncodeToString(sign)
}

type mockApiClient struct {
	mock.Mock
}

func (m *mockApiClient) ServiceTicket(srcId int, dstIds []int, secret []byte) (map[int]string, error) {
	args := m.Called(srcId, dstIds, secret)

	var res map[int]string
	if v, ok := args.Get(0).(map[int]string); ok {
		res = v
	}
	return res, args.Error(1)
}

func (m *mockApiClient) GetKeys() (map[uint32]*rw.PublicKey, error) {
	args := m.Called()
	var res map[uint32]*rw.PublicKey
	if v, ok := args.Get(0).(map[uint32]*rw.PublicKey); ok {
		res = v
	}
	return res, args.Error(1)
}
