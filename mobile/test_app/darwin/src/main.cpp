#include <QtWidgets/QApplication>
#include <QtWidgets/QListWidget>

#include <yandex/metrokit/platform/scheme_view/qt/scheme_view.h>
#include <yandex/metrokit/platform/metrokit/metrokit_factory.h>
#include <yandex/metrokit/platform/metrokit/metrokit.h>
#include <yandex/metrokit/platform/l10n_manager.h>
#include <yandex/metrokit/platform/scheme_manager/scheme_list.h>
#include <yandex/metrokit/platform/progress.h>
#include <yandex/metrokit/platform/log.h>

#include <yandex/metrokit/variant.h>
#include <yandex/metrokit/container_utils.h>
#include <yandex/metrokit/log.h>

#include <iomanip>

using namespace ::yandex::metrokit;
using namespace ::yandex::metrokit::platform;
using namespace ::yandex::metrokit::platform::scheme_view::qt;

namespace {

const auto APP_TAG = "Darwin Test App";
const Language DEFAULT_LANGUAGE{"en"};
const CountryCode DEFAULT_COUNTRY{"US"};

auto bundleSizeToString(unsigned int size) -> std::string {
    if (size < 1024) {
        return std::to_string(size) + " B";
    }
    size /= 1024;
    if (size < 1024) {
        return std::to_string(size) + " KB";
    }
    size /= 1024;
    if (size < 1024) {
        return std::to_string(size) + " MB";
    }
    double dsize = size / 1024.0f;
    std::stringstream ss;
    ss << std::setprecision(2) << dsize;
    return ss.str() + " GB";
}

auto buildSchemeRecordString(const scheme::SchemeSummary& schemeSummary) -> QString {
    QString result;

    result += getStringWithFallback(schemeSummary.name, DEFAULT_LANGUAGE).c_str();

    if (schemeSummary.updateInfo) {
        if (schemeSummary.isLocal) {
            result += " (update available: ";
        } else {
            result += " (download available: ";
        }
        result += bundleSizeToString(schemeSummary.updateInfo->downloadSize).c_str();
        result += ")";
    }

    return result;
}

} // namespace

class ViewController : public QObject {
Q_OBJECT
public:
    explicit ViewController(SchemeView* schemeView, QListWidget* schemeListWidget, QObject* parent = nullptr)
        : QObject(parent), schemeView_(schemeView), schemeListWidget_(schemeListWidget),
          schemeManager_(metrokit::builder()->build(DEFAULT_LANGUAGE, DEFAULT_COUNTRY)->createSchemeManager()), schemeList_(),
          schemeListUpdateListener_(), schemeListObtainmentListener_(), schemeUpdateListener_(),
          schemeObtainmentListener_()
    {
        updateSchemeListUi();
    }

public slots:
    void onSchemeListDoubleClicked(const QModelIndex& index) {
        const auto schemeSummary = schemeList_.at(index.row());
        updateScheme(schemeSummary);
    }

    void onUpdateButtonClicked() {
        updateSchemeList();
    }

    void onCenterButtonClicked() {
        centerSchemeView();
    }

    void updateSchemeUi(const scheme::SchemeId& schemeId) {
        const auto getSchemeSession = schemeManager_->scheme(schemeId);

        class Listener : public scheme_manager::SchemeObtainmentSessionListener {
        public:
            Listener(ViewController* viewController)
                : viewController_(viewController) {}

            void onSchemeObtainmentResult(const std::shared_ptr<scheme_manager::Scheme>& scheme) override {
                viewController_->schemeView_->delegate()->window()->surfaceController()->setScheme(
                    scheme, scheme->defaultLanguage());
            }

            void onSchemeObtainmentError(const scheme_manager::Error& error) override {
                log::error(APP_TAG, error.message);
            }

        private:
            ViewController* viewController_;
        };

        schemeObtainmentListener_ = std::make_shared<Listener>(this);

        getSchemeSession->addListener(schemeObtainmentListener_);
    }

public:
    SchemeView* schemeView_;
    QListWidget* schemeListWidget_;
    std::unique_ptr<scheme_manager::SchemeManager> schemeManager_;

    std::vector<scheme::SchemeSummary> schemeList_;
    std::shared_ptr<scheme_manager::SchemeListUpdatingSessionListener> schemeListUpdateListener_;
    std::shared_ptr<scheme_manager::SchemeListObtainmentSessionListener> schemeListObtainmentListener_;
    std::shared_ptr<scheme_manager::SchemeUpdatingSessionListener> schemeUpdateListener_;
    std::shared_ptr<scheme_manager::SchemeObtainmentSessionListener> schemeObtainmentListener_;

    void centerSchemeView() {
        const auto cameraController = schemeView_->delegate()->window()->cameraController();
        const auto oldCamera = cameraController->camera();
        const auto newCamera = std::make_shared<scheme_window::camera::Camera>(
            oldCamera->scale,
            geometry::Point { 0.0f, 0.0f }
        );
        cameraController->setCamera(newCamera, Animation { Animation::Type::Linear, 0.2f });
    }

    void updateSchemeList() {
        const auto updateSchemeListSession = schemeManager_->updateSchemeList();

        class Listener : public scheme_manager::SchemeListUpdatingSessionListener {
        public:
            Listener(ViewController* viewController)
                : viewController_(viewController) {}

            void onSchemeListUpdateResult() override {
                viewController_->updateSchemeListUi();
            }

            void onSchemeListUpdateError(const scheme_manager::Error& error) override {
                log::error(APP_TAG, error.message);
            }

        private:
            ViewController* viewController_;
        };

        schemeListUpdateListener_ = std::make_shared<Listener>(this);

        updateSchemeListSession->addListener(schemeListUpdateListener_);
    }

    void updateScheme(const scheme::SchemeSummary& schemeSummary) {
        const auto schemeId = schemeSummary.schemeId;
        if (schemeSummary.updateInfo) {
            const auto updateSchemeSession = schemeManager_->updateScheme(schemeId);

            class Listener : public scheme_manager::SchemeUpdatingSessionListener {
            public:
                Listener(ViewController* viewController, const scheme::SchemeId& schemeId)
                    : viewController_(viewController), schemeId_(schemeId) {}

                void onSchemeUpdateResult(const std::shared_ptr<scheme::SchemeSummary>& schemeSummary) override {
                    log::debug(APP_TAG, "Scheme updated: " + schemeSummary->schemeId.value);
                    viewController_->updateSchemeList();
                    viewController_->updateSchemeUi(schemeId_);
                }

                void onSchemeUpdateProgress(const Progress& progress) override {
                    log::debug(APP_TAG, "Updating progress: " + StringConvertible<float>::toString(progress.value));
                }

                void onSchemeUpdateError(const scheme_manager::Error& error) override {
                    log::error(APP_TAG, error.message);
                }

            private:
                ViewController* viewController_;
                scheme::SchemeId schemeId_;
            };

            schemeUpdateListener_ = std::make_shared<Listener>(this, schemeId);

            updateSchemeSession->addListener(schemeUpdateListener_);
        } else {
            updateSchemeUi(schemeId);
        }
    }

    void updateSchemeListUi() {
        const auto getSchemeListSession = schemeManager_->schemeList(false);

        class Listener : public scheme_manager::SchemeListObtainmentSessionListener {
        public:
            Listener(ViewController* viewController)
                : viewController_(viewController) {}

            void onSchemeListObtainmentResult(const std::shared_ptr<scheme_manager::SchemeList>& schemeList) override {
                viewController_->schemeList_.clear();

                for (const auto& item : *schemeList->items) {
                    const auto& schemes = *item.schemes;
                    viewController_->schemeList_.insert(
                        viewController_->schemeList_.end(), schemes.begin(), schemes.end());
                }

                QStringList schemeItems;
                for (const auto& summary : viewController_->schemeList_) {
                    schemeItems << buildSchemeRecordString(summary);
                }

                viewController_->schemeListWidget_->clear();
                viewController_->schemeListWidget_->addItems(schemeItems);
            }

            void onSchemeListObtainmentError(const scheme_manager::Error& error) override {
                log::error(APP_TAG, error.message);
            }

        private:
            ViewController* viewController_;
        };

        schemeListObtainmentListener_ = std::make_shared<Listener>(this);

        getSchemeListSession->addListener(schemeListObtainmentListener_);
    }
};

auto setupViewController(
    SchemeView* schemeView,
    QListWidget* schemeListWidget,
    QWidget* centerButton,
    QWidget* updateButton)
    -> std::unique_ptr<ViewController>
{
    auto viewController = std::make_unique<ViewController>(schemeView, schemeListWidget);

    QObject::connect(
        schemeListWidget,
        SIGNAL(doubleClicked(QModelIndex)),
        viewController.get(),
        SLOT(onSchemeListDoubleClicked(QModelIndex))
    );

    QObject::connect(
        centerButton,
        SIGNAL(clicked()),
        viewController.get(),
        SLOT(onCenterButtonClicked())
    );

    QObject::connect(
        updateButton,
        SIGNAL(clicked()),
        viewController.get(),
        SLOT(onUpdateButtonClicked())
    );

    return viewController;
}

int main(int argc, char* argv[]) {
    getLoggerInstance()->setIsStdWriterEnabled(true);

    QApplication app(argc, argv);
    app.setApplicationVersion("0.0.1");

    auto* schemeView = new SchemeView;
    schemeView->setSizePolicy(QSizePolicy::Policy::Expanding, QSizePolicy::Policy::Expanding);

    auto* schemeListWidget = new QListWidget;
    schemeListWidget->setSizePolicy(QSizePolicy::Policy::Fixed, QSizePolicy::Policy::Expanding);

    auto* centerButton = new QPushButton;
    centerButton->setText("Center camera");

    auto* updateButton = new QPushButton;
    updateButton->setText("Update scheme list");

    auto* schemeListLayout = new QVBoxLayout;
    schemeListLayout->addWidget(centerButton);
    schemeListLayout->addWidget(updateButton);
    schemeListLayout->addWidget(schemeListWidget);

    auto* mainLayout = new QHBoxLayout;
    mainLayout->addWidget(schemeView);
    mainLayout->addLayout(schemeListLayout);

    QWidget window;
    window.setLayout(mainLayout);
    window.show();

    auto viewController = setupViewController(schemeView, schemeListWidget, centerButton, updateButton);

    return app.exec();
}

// Generated file. Do not delete
#include "main.moc"
