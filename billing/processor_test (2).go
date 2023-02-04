package core

import (
	"errors"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testSuite struct {
	btesting.BaseSuite
}

func (s *testSuite) TestProcessorResponseFatalErrorErr() {
	err := errors.New(btesting.RandS(16))
	response := ProcessorResponse{Err: err, Code: 500}
	assert.Equal(s.T(), fmt.Sprintf("%s: %s", err.Error(), err.Error()), response.FatalError().Error())
}

func (s *testSuite) TestProcessorResponseFatalErrorStatus() {
	response := ProcessorResponse{Code: 500, Status: btesting.RandS(16)}
	expected := fmt.Sprintf(
		"response code=%d, status=%s: response code=%d, status=%s",
		response.Code, response.Status, response.Code, response.Status,
	)
	assert.Equal(
		s.T(),
		expected,
		response.FatalError().Error(),
	)
}

func (s *testSuite) TestProcessorResponseNoError() {
	response := ProcessorResponse{Err: errors.New(btesting.RandS(16))}
	var expected *ProcessorMessage = nil
	assert.Equal(s.T(), expected, response.Error())
}

func (s *testSuite) TestProcessorResponseNoFatalError() {
	response := ProcessorResponse{Code: 400}
	assert.Equal(s.T(), response.Error(), &ProcessorMessage{
		Type: ProcessorMessageTypeError,
		Payload: map[string]any{
			"request": response.Request,
			"error":   response.Data,
		},
	})
}

func TestProcessor(t *testing.T) {
	suite.Run(t, &testSuite{})
}
