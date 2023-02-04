package mock

import (
	"errors"
	"fmt"
	"math/rand"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/telegram/bot"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/test"
	tg "github.com/go-telegram-bot-api/telegram-bot-api"
	"github.com/stretchr/testify/assert"
)

type TelegramMock struct {
	rand *rand.Rand
	log  logger.Logger

	IN                 chan bot.InputMessage
	OUT                chan *bot.OutputMessage
	Error              error
	SendCount          int
	NumDuplicateErrors int

	messageId   int
	state       map[msgKey]*MsgInfo
	chatHistory map[int64][]*MsgInfo
	mx          sync.Mutex
	startC      chan struct{}
}

type msgKey struct {
	ChatID    int64
	MessageID int
}

type MsgInfo struct {
	Text             string
	ReplyToMessageID int
	Buttons          []*bot.Button
}

func NewTelegramMock(t *testing.T) *TelegramMock {
	return &TelegramMock{
		rand: rand.New(rand.NewSource(time.Now().UnixNano())),
		log:  test.NewLogger(t).NewContext("mock", "telegram", logger.External),
		IN:   make(chan bot.InputMessage, 10),
		OUT:  make(chan *bot.OutputMessage, 10),

		state:       make(map[msgKey]*MsgInfo),
		chatHistory: make(map[int64][]*MsgInfo),
		startC:      make(chan struct{}),
	}
}

func (m *TelegramMock) AddMessage(chatID int64, msg *MsgInfo) int {
	m.mx.Lock()
	defer m.mx.Unlock()
	m.messageId++

	key := msgKey{ChatID: chatID, MessageID: m.messageId}
	_, exists := m.state[key]
	m.state[key] = msg
	if !exists {
		m.chatHistory[key.ChatID] = append(m.chatHistory[key.ChatID], msg)
	}
	return m.messageId
}

func (m *TelegramMock) History(chatID int64) []*MsgInfo {
	m.mx.Lock()
	defer m.mx.Unlock()
	if hist, ok := m.chatHistory[chatID]; ok {
		result := make([]*MsgInfo, len(hist))
		copy(result, hist)
		return result
	}
	return nil
}

func (m *TelegramMock) Send(msg *bot.OutputMessage) error {
	m.SendCount++

	if m.Error != nil {
		err := m.Error
		m.Error = nil
		return err
	}

	m.log.Info("Send: ", msg.Message)
	if err := m.sendOutput(msg); err != nil {
		m.log.Info("output err")
		return err
	}
	m.OUT <- msg
	return nil
}

func (m *TelegramMock) sendOutput(msg *bot.OutputMessage) error {
	chattable := msg.MakeChattable()
	m.mx.Lock()
	defer m.mx.Unlock()

	switch sendAction := chattable.(type) {
	case *tg.MessageConfig:
		key := msgKey{ChatID: sendAction.ChatID, MessageID: rand.Int()}
		saveMsg := &MsgInfo{
			Text:             sendAction.Text,
			ReplyToMessageID: sendAction.ReplyToMessageID,
		}
		m.state[key] = saveMsg
		m.chatHistory[key.ChatID] = append(m.chatHistory[key.ChatID], saveMsg)
		msg.TgMsg = &tg.Message{
			Chat:      &tg.Chat{ID: sendAction.ChatID},
			MessageID: key.MessageID,
			Text:      sendAction.Text,
		}
	case *tg.EditMessageTextConfig:
		key := msgKey{ChatID: sendAction.ChatID, MessageID: sendAction.MessageID}
		curState, ok := m.state[key]
		switch {
		case !ok:
			return errors.New("Bad Request: message to edit not found")
		case curState.Text == sendAction.Text && len(curState.Buttons) == len(msg.Buttons):
			m.NumDuplicateErrors++
			return errNotModified
		default:
			curState.Text = sendAction.Text
			curState.Buttons = msg.Buttons
			msg.TgMsg = &tg.Message{
				Chat:      &tg.Chat{ID: sendAction.ChatID},
				MessageID: key.MessageID,
				Text:      sendAction.Text,
			}
			if curState.ReplyToMessageID != 0 {
				msg.TgMsg.ReplyToMessage = &tg.Message{
					MessageID: curState.ReplyToMessageID,
				}
			}
		}
	default:
		return fmt.Errorf("unsupported msg type '%T", chattable)
	}
	return nil
}

func (m *TelegramMock) Start() {
	close(m.startC)
}

func (m *TelegramMock) Stop() {
}

func (m *TelegramMock) CheckChatAccess(_ int64) (bool, error) {
	return true, nil
}

func (m *TelegramMock) C() chan bot.InputMessage {
	<-m.startC
	return m.IN
}

func NewMigrateInputMessageMock(login string, fromChatID int64, toChatID int64) bot.InputMessage {
	origin := &tg.Message{
		Text: "",
		Chat: &tg.Chat{
			ID: toChatID,
		},
		From: &tg.User{
			UserName: login,
		},
		MigrateFromChatID: fromChatID,
	}
	update := &tg.Update{
		Message: origin,
	}
	return bot.NewNewMessage(update)
}

func NewInputMessageMock(msg, login string, chatID int64) bot.InputMessage {
	origin := &tg.Message{
		Text: msg,
		Chat: &tg.Chat{
			ID: chatID,
		},
		From: &tg.User{
			UserName: login,
		},
	}
	update := &tg.Update{
		Message: origin,
	}
	return bot.NewNewMessage(update)
}

func NewCallbackInputMessageMock(msg, login string, chatID int64) *bot.CallbackMessage {
	update := &tg.Update{
		CallbackQuery: &tg.CallbackQuery{
			Data: msg,
			From: &tg.User{
				UserName: login,
			},
			Message: &tg.Message{
				Chat: &tg.Chat{
					ID: chatID,
				},
			},
		},
	}
	return bot.NewCallbackMessage(update)
}

func (m *TelegramMock) Read(t *testing.T, count int) []*bot.OutputMessage {
	var result []*bot.OutputMessage
	for i := 0; i < count; i++ {
		select {
		case out := <-m.OUT:
			result = append(result, out)
		case <-time.NewTimer(5 * time.Second).C:
			assert.FailNow(t, fmt.Sprintf("timeout: wait %d, but read only %d messages", count, len(result)))
		}
	}
	time.Sleep(100 * time.Millisecond)
	select {
	case out := <-m.OUT:
		assert.FailNow(t, "more message: ", out.Message)
	default:
	}
	return result
}

func (m *TelegramMock) Get(t *testing.T) *bot.OutputMessage {
	read := m.Read(t, 1)
	return read[0]
}

var (
	errNotModified = errors.New("Bad Request: message is not modified: specified new message content and reply markup are exactly the same as a current content and reply markup of the message")
)
