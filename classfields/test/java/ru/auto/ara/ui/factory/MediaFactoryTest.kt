package ru.auto.ara.ui.factory

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.types.beOfType
import ru.auto.data.model.Size
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.Photo
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.data.offer.State
import ru.auto.data.model.data.offer.Video
import ru.auto.data.model.data.offer.panorama.ExteriorPanorama
import ru.auto.data.model.data.offer.panorama.ExteriorPanoramas
import ru.auto.data.model.data.offer.panorama.InteriorPanorama
import ru.auto.data.model.data.offer.panorama.PanoramaPicture
import ru.auto.data.model.data.offer.panorama.PanoramaVideo
import ru.auto.data.model.data.offer.panorama.TileLevel
import ru.auto.data.model.data.offer.panorama.YandexMaps

/**
 *
 * exteriorPanorama - e
 * interiorPanorama - i
 * video - v
 * photos - p
 * firstPhoto - fp
 * otherPhotos(without fp) - op
 *
 * Priority e i v p, but i v -> v i, i p -> fp i op, v p -> fp v op, i v p -> fp i v op
 *
 * Cases:
 *   empty
 *   e
 *   i
 *   v
 *   p
 *   e i
 *   e v
 *   e p
 *   i v -> v i
 *   i p -> fp i, fp i op
 *   v p -> fp v, fp v op
 *   e i v
 *   e i p
 *   e v p
 *   i v p ->  fp i v, fp i v op
 *   e i v p
 */
class MediaFactoryTest : FreeSpec(
    {

        val url = "https://test.ru"

        val offer = Offer(
            category = VehicleCategory.CARS,
            id = "8888888888888888888-88888888",
            sellerType = SellerType.PRIVATE
        )

        val firstPhoto = Photo(name = "firstPhoto", medium = url)
        val secondPhoto = Photo(name = "secondPhoto", medium = url)

        val photos = listOf(firstPhoto, secondPhoto)

        val video = Video(url = url, previews = Photo(name = "video", small = url))

        val exteriorPanoramas = ExteriorPanoramas(
            published = ExteriorPanorama(
                id = "exteriorPanorama",
                videoMP4 = PanoramaVideo(previewUrl = url, fullUrl = url, lowResUrl = url),
                pictureWebp = PanoramaPicture(count = 1 , previewFirstFrameUrl = url, fullFirstFrameUrl = url)
            )
        )

        val interiorPanoramas = listOf(
            InteriorPanorama(
                id = "interiorPanorama",
                previewUrl = url,
                yandexMaps = YandexMaps(
                    tileSize = Size(width = 256, height = 256),
                    highQuality = TileLevel(
                        size = Size(width = 5376, height = 2688),
                        firstTile = url
                    ),
                    lowQuality = TileLevel(
                        size = Size(width = 256, height = 128),
                        firstTile = url
                    )
                )
            )
        )

        fun <T> Collection<T>.should(matchers: List<Matcher<T>>) {
            this shouldHaveSize matchers.size
            forEachIndexed { index, item -> item should matchers[index] }
        }

        fun checkBuildMedia(state: State, matchers: List<Matcher<Any?>>) {
            MediaFactory.build(offer = offer.copy(state = state)).should(matchers)
        }

        "should build media from offer" - {

            "empty" {
                checkBuildMedia(
                    state = State(),
                    matchers = emptyList()
                )
            }

            "exteriorPanorama" {
                checkBuildMedia(
                    state = State(exteriorPanoramas = exteriorPanoramas),
                    matchers = listOf(beOfType<ExteriorPanorama>())
                )
            }

            "interiorPanorama" {
                checkBuildMedia(
                    state = State(interiorPanoramas = interiorPanoramas),
                    matchers = listOf(beOfType<InteriorPanorama>())
                )
            }

            "video" {
                checkBuildMedia(
                    state = State(video = video),
                    matchers = listOf(beOfType<Video>())
                )
            }

            "photos" {
                checkBuildMedia(
                    state = State(images = photos),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<Photo>()
                    )
                )
            }

            "exteriorPanorama + interiorPanorama" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        interiorPanoramas = interiorPanoramas
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<InteriorPanorama>()
                    )
                )
            }

            "exteriorPanorama + video" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        video = video
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<Video>()
                    )
                )
            }

            "exteriorPanorama + photos" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<Photo>(),
                        beOfType<Photo>()
                    )
                )
            }

            "video + interiorPanorama" {
                checkBuildMedia(
                    state = State(
                        interiorPanoramas = interiorPanoramas,
                        video = video
                    ),
                    matchers = listOf(
                        beOfType<Video>(),
                        beOfType<InteriorPanorama>()
                    )
                )
            }

            "firstPhoto + interiorPanorama" {
                checkBuildMedia(
                    state = State(
                        interiorPanoramas = interiorPanoramas,
                        images = listOf(firstPhoto)
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<InteriorPanorama>()
                    )
                )
            }

            "firstPhoto + interiorPanorama + otherPhotos" {
                checkBuildMedia(
                    state = State(
                        interiorPanoramas = interiorPanoramas,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Photo>()
                    )
                )
            }

            "firstPhoto + video" {
                checkBuildMedia(
                    state = State(
                        video = video,
                        images = listOf(firstPhoto)
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<Video>()
                    )
                )
            }

            "firstPhoto + video + otherPhotos" {
                checkBuildMedia(
                    state = State(
                        video = video,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<Video>(),
                        beOfType<Photo>()
                    )
                )
            }

            "exteriorPanorama + interiorPanorama + video" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        interiorPanoramas = interiorPanoramas,
                        video = video
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Video>()
                    )
                )
            }

            "exteriorPanorama + interiorPanorama + photos" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        interiorPanoramas = interiorPanoramas,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Photo>(),
                        beOfType<Photo>()
                    )
                )
            }

            "exteriorPanorama + video + photos" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        video = video,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<Video>(),
                        beOfType<Photo>(),
                        beOfType<Photo>()
                    )
                )
            }

            "firstPhoto + interiorPanorama + video" {
                checkBuildMedia(
                    state = State(
                        interiorPanoramas = interiorPanoramas,
                        video = video,
                        images = listOf(firstPhoto)
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Video>()
                    )
                )
            }

            "firstPhoto + interiorPanorama + video + otherPhotos" {
                checkBuildMedia(
                    state = State(
                        interiorPanoramas = interiorPanoramas,
                        video = video,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<Photo>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Video>(),
                        beOfType<Photo>()
                    )
                )
            }

            "exteriorPanorama + interiorPanorama + video + photos" {
                checkBuildMedia(
                    state = State(
                        exteriorPanoramas = exteriorPanoramas,
                        interiorPanoramas = interiorPanoramas,
                        video = video,
                        images = photos
                    ),
                    matchers = listOf(
                        beOfType<ExteriorPanorama>(),
                        beOfType<InteriorPanorama>(),
                        beOfType<Video>(),
                        beOfType<Photo>(),
                        beOfType<Photo>()
                    )
                )
            }
        }
    }
)
