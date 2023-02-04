package pusher

import (
	"github.com/YandexClassifieds/goLB"
)

type mockProducer struct {
	closeSign    chan bool
	success      bool
	backupData   []*goLB.Data
	successCount int
	failCount    int
}

func mock(state bool) (*mockProducer, goLB.TokenProvider) {

	return &mockProducer{
		closeSign:  make(chan bool),
		backupData: []*goLB.Data{},
		success:    state,
	}, MockProvider{}
}

func (m *mockProducer) Init() (maxSeqNo uint64, err error) {
	return 10, nil
}

func (m *mockProducer) Close() {
	close(m.closeSign)
}

func (m *mockProducer) Push(d *goLB.Data) {
	if m.success {
		d.Success()
		m.successCount++
	} else {
		d.Fail()
		m.failCount++
	}
}

func (m *mockProducer) CloseSign() <-chan bool {
	return m.closeSign
}

func (m *mockProducer) Backup() []*goLB.Data {

	return m.backupData
}

type MockProvider struct {
}

func (MockProvider) Type() goLB.TokenType {
	return goLB.TokenOAuth
}

func (MockProvider) Token() []byte {
	return []byte{}
}
