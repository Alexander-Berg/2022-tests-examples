package fields_test

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/cli/commands/format/fields"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestEmpty(t *testing.T) {
	field := &fields.Rest{}
	field.Init(logrus.New())
	res := fmt.Sprintf(field.SlimFormat(), field.SlimValue(&core.LogMessage{}))

	require.Equal(t, "", res)
}

func TestSimple(t *testing.T) {
	field := &fields.Rest{}
	field.Init(logrus.New())
	res := fmt.Sprintf(field.SlimFormat(), field.SlimValue(&core.LogMessage{
		Rest: `{"flowId":"_c_b2f3e771f89a1efa6459e3cd1622cd34-flow0000000542"}`,
	}))

	require.Equal(t, "\n  {flowId: _c_b2f3e771f89a1efa6459e3cd1622cd34-flow0000000542}", res)
}
