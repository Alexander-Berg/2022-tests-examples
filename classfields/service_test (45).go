package command

import (
	"fmt"
	"github.com/YandexClassifieds/shiva/pkg/links"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/telegram/command/common"
	"github.com/YandexClassifieds/shiva/cmd/telegram/subscribe"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestRun(t *testing.T) {
	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, name, labels, _, err := s.parseMessage("/run -l prod service-name")
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "prod", labels[common.LayerLabel])
	assert.Equal(t, "service-name", name)
}

func TestRunForce(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, name, labels, _, err := s.parseMessage("/run -l prod -force -v 1.0 service-name")
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "prod", labels[common.LayerLabel])
	assert.Equal(t, "true", labels[common.ForceLabel])
	assert.Equal(t, "1.0", labels[common.VersionLabel])
	assert.Equal(t, "service-name", name)
}

func TestRunWithOverride(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, name, labels, stackingLabels, err := s.parseMessage(`/run -l test -b branch -e e1:t1 -e "e2:t 2" -co /conf/test.yml -e s1:${sec-43:ver-45123:aaa} service-name`)
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "test", labels[common.LayerLabel])
	assert.Equal(t, "branch", labels[common.BranchLabel])
	assert.Len(t, stackingLabels[common.ConfOverrideLabel], 1)
	assert.Len(t, stackingLabels[common.EnvOverrideLabel], 3)
	assert.Contains(t, stackingLabels[common.EnvOverrideLabel], "e1:t1")
	assert.Contains(t, stackingLabels[common.EnvOverrideLabel], "e2:t 2")
	assert.Contains(t, stackingLabels[common.ConfOverrideLabel], "/conf/test.yml")
	assert.Contains(t, stackingLabels[common.EnvOverrideLabel], "s1:${sec-43:ver-45123:aaa}")
	assert.Equal(t, "service-name", name)
}

func TestPromote(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, _, labels, _, err := s.parseMessage("/promote -id 123")
	require.NoError(t, err)
	assert.Equal(t, "promote", command.Name)
	assert.Equal(t, "123", labels[common.IDLabel])
}

func TestApproveList(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, _, _, _, err := s.parseMessage("/approve_list")
	require.NoError(t, err)
	assert.Equal(t, "approve_list", command.Name)
}

func TestSubs(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, _, _, _, err := s.parseMessage("/subs")
	require.NoError(t, err)
	assert.Equal(t, "subs", command.Name)
}

func TestParseCommandWithBotLogin(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, service, labels, _, err := s.parseMessage("/run@vertis_shiva_bot -l test -v 1.6.5 -t golp-log-generator")
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "golp-log-generator", service)
	assert.Equal(t, "test", labels[common.LayerLabel])
	assert.Equal(t, "1.6.5", labels[common.VersionLabel])
	assert.Equal(t, "true", labels[common.TrafficShareLabel])
}

func TestSub(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, service, labels, _, err := s.parseMessage("/sub -b -l prod shiva-test")
	require.NoError(t, err)
	assert.Equal(t, "sub", command.Name)
	assert.Equal(t, "shiva-test", service)
	assert.Equal(t, "prod", labels[common.LayerLabel])
	assert.Equal(t, "true", labels[common.IsBranchLabel])
}

func TestUnsub(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, service, labels, _, err := s.parseMessage("/unsub -l test shiva-test")
	require.NoError(t, err)
	assert.Equal(t, "unsub", command.Name)
	assert.Equal(t, "shiva-test", service)
	assert.Equal(t, "test", labels[common.LayerLabel])
}

func TestParseCommandDoubleSpace(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, service, labels, _, err := s.parseMessage("run -l  test -v  1.6.5  -t   golp-log-generator")
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "golp-log-generator", service)
	assert.Equal(t, 3, len(labels))
	assert.Equal(t, "test", labels[common.LayerLabel])
	assert.Equal(t, "1.6.5", labels[common.VersionLabel])
	assert.Equal(t, "true", labels[common.TrafficShareLabel])
}

func TestParseComment(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	type tCase struct {
		name           string
		comment        string
		overrideResult string
	}
	tCases := []tCase{
		{
			name:    "short",
			comment: "Hi!",
		},
		{
			name:    "long_err",
			comment: "Fix my stupid",
		},
		{
			name:           "label",
			comment:        "new -l flag",
			overrideResult: "new",
		},
		{
			name:           "label_quote",
			comment:        "\"new -l flag\"",
			overrideResult: "new -l flag",
		},
		{
			name:           "long_quote",
			comment:        "\"Fix my stupid\"",
			overrideResult: "Fix my stupid",
		},
		{
			name:    "markdown_block",
			comment: "`markdown`",
		},
		{
			name:    "markdown_block",
			comment: "```markdown```",
		},
		{
			name:           "markdown_block_quote",
			comment:        "\"```markdown```\"",
			overrideResult: "```markdown```",
		},
		{
			name:    "long_markdown",
			comment: "```markdown markdown markdown```",
		},
		{
			name:           "long_markdown_quote",
			comment:        "\"```Hi *bro*\n buy!```\"",
			overrideResult: "```Hi *bro*\n buy!```",
		},
		{
			name:           "escape_quote",
			comment:        `"a \"b\" c"`,
			overrideResult: `a "b" c`,
		},
		{
			name:    "inner_quote",
			comment: "hi \" hi",
		},
		{
			name:           "quote_1",
			comment:        `"test test"`,
			overrideResult: `test test`,
		},
		{
			name:           "quote_2",
			comment:        `'test test'`,
			overrideResult: `test test`,
		},
		{
			name:           "quote_3",
			comment:        `“test test”`,
			overrideResult: `test test`,
		},
		{
			name:           "quote_4",
			comment:        `«test test»`,
			overrideResult: `test test`,
		},
	}
	for _, c := range tCases {
		t.Run(c.name, func(t *testing.T) {
			_, _, labels, _, err := s.parseMessage(fmt.Sprintf("run -l test -v 1.6.5 -c %s my_service", c.comment))
			result := c.comment
			if c.overrideResult != "" {
				result = c.overrideResult
			}
			require.NoError(t, err)
			assert.Equal(t, result, labels[common.CommentLabel])
		})
	}
}

func TestParseCommandDifferentCase(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	command, service, labels, _, err := s.parseMessage("rUn -l  tEsT -v  1.6.5-FeAtUrE   golp-log-gEnErAtOr")
	require.NoError(t, err)
	assert.Equal(t, "run", command.Name)
	assert.Equal(t, "golp-log-gEnErAtOr", service)
	assert.Equal(t, 2, len(labels))
	assert.Equal(t, "test", labels[common.LayerLabel])
	assert.Equal(t, "1.6.5-FeAtUrE", labels[common.VersionLabel])
}

func TestHelpCommandOutput(t *testing.T) {

	test.InitTestEnv()
	s := NewService(subscribe.NewService(test_db.NewDb(t), nil, test.NewLogger(t)), nil)
	result := help(s.commands)

	expectation := `/run -l test -v 0.1.7 service_name
/restart -l test service_name
/revert -l test service_name
/stop -l test service_name
/status service_name
/sub -l test service_name
/unsub -l test service_name
/subs
/system_status

More: ` + links.TelegramBot + `
Full docs: ` + links.BaseDomain

	assert.Equal(t, expectation, result)
}
