package message

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
)

func TestUserErrorMessage(t *testing.T) {
	s := NewService(test.NewLogger(t), &NdaMock{}, NewMarkdown())
	cases := []struct {
		name        string
		err         *user_error.UserError
		expectedMsg string
	}{
		{
			name: "withRuMsg",
			err:  user_error.NewUserError(fmt.Errorf("error"), "Описание ошибки", "link1", "link2"),
			expectedMsg: s.BacktickHack(`
Error: ¬Описание ошибки¬
Docs:
 • link1
 • link2`),
		},
		{
			name: "withOutRuMsg",
			err:  user_error.NewUserError(fmt.Errorf("error"), ""),
			expectedMsg: s.BacktickHack(`
Error: ¬error¬`),
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			assert.Equal(t, c.expectedMsg, s.RenderUserError(c.err))
		})
	}
}

type NdaMock struct {
}

func (n *NdaMock) NdaUrl(url string) (string, error) {
	return url, nil
}
