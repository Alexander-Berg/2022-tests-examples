// Code generated by mockery v2.12.2. DO NOT EDIT.

package ssh

import (
	mock "github.com/stretchr/testify/mock"
	cryptossh "golang.org/x/crypto/ssh"

	testing "testing"

	time "time"
)

// IClient is an autogenerated mock type for the IClient type
type IClient struct {
	mock.Mock
}

// ConcurrentRun provides a mock function with given fields: remoteHosts, command
func (_m *IClient) ConcurrentRun(remoteHosts []string, command string) error {
	ret := _m.Called(remoteHosts, command)

	var r0 error
	if rf, ok := ret.Get(0).(func([]string, string) error); ok {
		r0 = rf(remoteHosts, command)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}

// Connect provides a mock function with given fields: host
func (_m *IClient) Connect(host string) (*cryptossh.Session, error) {
	ret := _m.Called(host)

	var r0 *cryptossh.Session
	if rf, ok := ret.Get(0).(func(string) *cryptossh.Session); ok {
		r0 = rf(host)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(*cryptossh.Session)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(string) error); ok {
		r1 = rf(host)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// Run provides a mock function with given fields: session, command
func (_m *IClient) Run(session *cryptossh.Session, command string) (string, error) {
	ret := _m.Called(session, command)

	var r0 string
	if rf, ok := ret.Get(0).(func(*cryptossh.Session, string) string); ok {
		r0 = rf(session, command)
	} else {
		r0 = ret.Get(0).(string)
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(*cryptossh.Session, string) error); ok {
		r1 = rf(session, command)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// RunWithConfirm provides a mock function with given fields: session, command, confirmTimeout, confrimString
func (_m *IClient) RunWithConfirm(session *cryptossh.Session, command string, confirmTimeout time.Duration, confrimString string) (string, error) {
	ret := _m.Called(session, command, confirmTimeout, confrimString)

	var r0 string
	if rf, ok := ret.Get(0).(func(*cryptossh.Session, string, time.Duration, string) string); ok {
		r0 = rf(session, command, confirmTimeout, confrimString)
	} else {
		r0 = ret.Get(0).(string)
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(*cryptossh.Session, string, time.Duration, string) error); ok {
		r1 = rf(session, command, confirmTimeout, confrimString)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// NewIClient creates a new instance of IClient. It also registers the testing.TB interface on the mock and a cleanup function to assert the mocks expectations.
func NewIClient(t testing.TB) *IClient {
	mock := &IClient{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
