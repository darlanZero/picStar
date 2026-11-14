package com.picstar.app.api

import android.graphics.BitmapFactory
import android.text.Html
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.HighlightCard
import com.picstar.app.model.IllustComment
import com.picstar.app.model.IllustItem
import com.picstar.app.model.IllustTag
import com.picstar.app.model.SearchFilters
import com.picstar.app.model.SearchWorkType
import com.picstar.app.model.UgoiraFrame
import com.picstar.app.model.UgoiraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Pixiv App-API & OAuth2 PKCE Client modeled after PixEz Flutter
 * (lib/network/api_client.dart & lib/network/oauth_client.dart).
 */
object PixEzApiClient {

    const val APP_API_HOST = "https://app-api.pixiv.net"
    const val OAUTH_HOST = "https://oauth.secure.pixiv.net"
    const val PXIMG_REFERER = "https://app-api.pixiv.net/"

    // Pixiv official Android client constants used by PixEz Flutter
    private const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private const val HASH_SALT = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private const val USER_AGENT = "PixivAndroidApp/5.0.234 (Android 14; Pixel 8 Pro)"

    private val bitmapCache = LruCache<String, ImageBitmap>(120)

    data class PkceSession(
        val codeVerifier: String,
        val codeChallenge: String,
        val loginUrl: String,
        val createAccountUrl: String
    )

    data class TokenResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val userName: String,
        val userAccount: String,
        val profileImageUrl: String
    )

    /**
     * Generates OAuth2 PKCE challenge and WebView login URL (PixEz pattern).
     */
    fun createPkceLoginSession(): PkceSession {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val verifier = Base64.encodeToString(
            randomBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val loginUrl = "$APP_API_HOST/web/v1/login?code_challenge=$challenge&code_challenge_method=S256&client=pixiv-android"
        val createAccountUrl = "$APP_API_HOST/web/v1/provisional-accounts/create?code_challenge=$challenge&code_challenge_method=S256&client=pixiv-android"
        return PkceSession(
            codeVerifier = verifier,
            codeChallenge = challenge,
            loginUrl = loginUrl,
            createAccountUrl = createAccountUrl
        )
    }

    /**
     * Generates X-Client-Time and X-Client-Hash (MD5 of ISO8601 time + Pixiv salt).
     */
    fun generateClientHeaders(): Map<String, String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val clientTime = sdf.format(Date())
        val md5Bytes = MessageDigest.getInstance("MD5")
            .digest((clientTime + HASH_SALT).toByteArray(Charsets.UTF_8))
        val clientHash = md5Bytes.joinToString("") { "%02x".format(it) }
        return mapOf(
            "User-Agent" to USER_AGENT,
            "App-OS" to "android",
            "App-OS-Version" to "14",
            "App-Version" to "5.0.234",
            "X-Client-Time" to clientTime,
            "X-Client-Hash" to clientHash,
            "Accept-Language" to "en_US"
        )
    }

    /**
     * Exchanges authorization code from `pixiv://account/login?code=...` for access & refresh tokens.
     */
    suspend fun exchangeAuthorizationCode(code: String, codeVerifier: String): Result<TokenResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val form = mapOf(
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET,
                    "code" to code,
                    "code_verifier" to codeVerifier,
                    "grant_type" to "authorization_code",
                    "include_policy" to "true",
                    "redirect_uri" to "$APP_API_HOST/web/v1/users/auth/pixiv/callback"
                )
                val json = postForm("$OAUTH_HOST/auth/token", form, null)
                parseTokenResponse(json)
            }
        }

    /**
     * Refreshes an existing refresh_token to obtain a fresh access_token.
     */
    suspend fun loginWithRefreshToken(refreshToken: String): Result<TokenResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val form = mapOf(
                    "client_id" to CLIENT_ID,
                    "client_secret" to CLIENT_SECRET,
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken.trim(),
                    "include_policy" to "true"
                )
                val json = postForm("$OAUTH_HOST/auth/token", form, null)
                parseTokenResponse(json)
            }
        }

    private fun parseTokenResponse(root: JSONObject): TokenResult {
        val response = root.optJSONObject("response") ?: root
        val accessToken = response.getString("access_token")
        val refreshToken = response.optString("refresh_token", "")
        val user = response.optJSONObject("user")
        val profileUrls = user?.optJSONObject("profile_image_urls")
        val profileImg = profileUrls?.optString("px_170x170")
            ?.ifBlank { profileUrls.optString("px_50x50") }
            ?: ""
        return TokenResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user?.optString("id") ?: "",
            userName = user?.optString("name") ?: "Pixiv User",
            userAccount = user?.optString("account") ?: "pixiv",
            profileImageUrl = profileImg
        )
    }

    /**
     * Fetches Pixivision Spotlights / Highlights (`/v1/spotlight/articles`).
     */
    suspend fun fetchSpotlightHighlights(accessToken: String): Result<List<HighlightCard>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "$APP_API_HOST/v1/spotlight/articles?filter=for_android&category=all"
                val json = getJson(endpoint, accessToken)
                val arr = json.optJSONArray("spotlight_articles") ?: JSONArray()
                val list = mutableListOf<HighlightCard>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "Pixiv Spotlight")
                    val category = obj.optString("pure_title", "SPOTLIGHT")
                    val subCategory = obj.optString("subcategory_label", "Pixivision")
                    val thumbnail = obj.optString("thumbnail", "").takeIf { it.isNotBlank() }
                    val publishDate = obj.optString("publish_date", "").take(10)
                    list.add(
                        HighlightCard(
                            id = obj.optLong("id", i.toLong()),
                            badge = "✦ $subCategory",
                            kicker = "PIXIVISION SPOTLIGHT · #${category.uppercase(Locale.US).take(18)}",
                            title = title,
                            authorName = "Pixivision Editorial",
                            authorInitials = "PX",
                            authorMeta = if (publishDate.isNotBlank()) "Published $publishDate" else "Featured today",
                            imageUrl = thumbnail,
                            articleUrl = obj.optString("article_url", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                list
            }
        }

    /**
     * Fetches recommended illustrations (`/v1/illust/recommended`) with `next_url` pagination.
     */
    suspend fun fetchRecommendedIllusts(
        accessToken: String,
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/illust/recommended?filter=for_android&include_ranking_illusts=true"
                val json = getJson(endpoint, accessToken)
                val illusts = parseIllustArray(json.optJSONArray("illusts"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                illusts to next
            }
        }

    /**
     * Fetches recommended artists (`/v1/user/recommended`).
     */
    suspend fun fetchRecommendedArtists(
        accessToken: String,
        nextUrl: String? = null
    ): Result<Pair<List<ArtistCard>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl ?: "$APP_API_HOST/v1/user/recommended?filter=for_android"
                val json = getJson(endpoint, accessToken)
                val artists = parseUserPreviews(json.optJSONArray("user_previews"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                artists to next
            }
        }

    /**
     * Fetches an artist's posted illustrations (`/v1/user/illusts`) so we can populate all 5 latest works!
     */
    suspend fun fetchArtistIllusts(
        accessToken: String,
        artistId: Long,
        type: String? = "illust"
    ): Result<List<IllustItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val typeParam = if (!type.isNullOrBlank()) "&type=$type" else ""
                val endpoint = "$APP_API_HOST/v1/user/illusts?filter=for_android&user_id=$artistId$typeParam"
                val json = getJson(endpoint, accessToken)
                parseIllustArray(json.optJSONArray("illusts"))
            }
        }

    /**
     * Fetches an artist's full profile details (`/v1/user/detail`), including bio, webpage, twitter, and counts.
     */
    suspend fun fetchArtistDetail(
        accessToken: String,
        artistId: Long
    ): Result<ArtistCard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "$APP_API_HOST/v1/user/detail?filter=for_android&user_id=$artistId"
                val json = getJson(endpoint, accessToken)
                val user = json.getJSONObject("user")
                val profile = json.optJSONObject("profile")
                val totalFollowUsers = profile?.optInt("total_follow_users", 0) ?: 0
                val totalMyPixiv = profile?.optInt("total_mypixiv_users", 0) ?: 0
                val webpage = profile?.optString("webpage", "")?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                val twitterUrl = profile?.optString("twitter_url", "")?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                val twitterAccount = profile?.optString("twitter_account", "")?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                val profileUrls = user.optJSONObject("profile_image_urls")
                ArtistCard(
                    id = user.optLong("id", artistId),
                    name = user.optString("name", "Artist"),
                    handle = "@${user.optString("account", "pixiv")}",
                    avatarUrl = profileUrls?.optString("medium"),
                    isFollowed = user.optBoolean("is_followed", false),
                    followersFormatted = formatCompactNumber(totalFollowUsers),
                    bio = cleanHtml(user.optString("comment", "")),
                    webpage = webpage,
                    twitterUrl = twitterUrl,
                    twitterAccount = twitterAccount,
                    totalMyPixivUsers = totalMyPixiv,
                    totalFollowUsers = totalFollowUsers
                )
            }
        }

    /**
     * Fetches MyPixiv users for an artist (`/v1/user/mypixiv`).
     */
    suspend fun fetchArtistMyPixiv(
        accessToken: String,
        artistId: Long,
        nextUrl: String? = null
    ): Result<Pair<List<ArtistCard>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/user/mypixiv?user_id=$artistId"
                val json = getJson(endpoint, accessToken)
                val artists = parseUserPreviews(json.optJSONArray("user_previews"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                artists to next
            }
        }

    /**
     * Fetches users following this artist (`/v1/user/follower`).
     */
    suspend fun fetchArtistFollowers(
        accessToken: String,
        artistId: Long,
        nextUrl: String? = null
    ): Result<Pair<List<ArtistCard>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/user/follower?filter=for_android&user_id=$artistId"
                val json = getJson(endpoint, accessToken)
                val artists = parseUserPreviews(json.optJSONArray("user_previews"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                artists to next
            }
        }

    /**
     * CORE FEATURE: Fetches "Other artists to follow" right after following an artist!
     * Endpoint from PixEz Flutter: `/v1/user/related?filter=for_android&seed_user_id={artistId}`
     */
    suspend fun fetchRelatedArtistsOnFollow(
        accessToken: String,
        seedArtistId: Long
    ): Result<List<ArtistCard>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "$APP_API_HOST/v1/user/related?filter=for_android&seed_user_id=$seedArtistId"
                val json = getJson(endpoint, accessToken)
                parseUserPreviews(json.optJSONArray("user_previews"))
            }
        }

    /**
     * Follows or unfollows an artist on Pixiv (`/v1/user/follow/add` or `/v1/user/follow/delete`).
     */
    suspend fun setFollowArtist(
        accessToken: String,
        artistId: Long,
        follow: Boolean,
        restrict: String = "public"
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (follow) "/v1/user/follow/add" else "/v1/user/follow/delete"
                val form = mutableMapOf("user_id" to artistId.toString())
                if (follow) form["restrict"] = restrict
                postForm("$APP_API_HOST$path", form, accessToken)
                Unit
            }
        }

    /**
     * Fetches rankings by mode (`day`, `day_male`, `day_female`, `week_original`, `week_rookie`,
     * `week`, `month`, `day_ai`, `day_r18_ai`, `day_r18`, `week_r18`, `week_r18g`).
     */
    suspend fun fetchRankingIllusts(
        accessToken: String,
        pixivMode: String,
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl ?: "$APP_API_HOST/v1/illust/ranking?filter=for_android&mode=$pixivMode"
                val json = getJson(endpoint, accessToken)
                val list = parseIllustArray(json.optJSONArray("illusts"), ranked = true)
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Fetches latest illustrations from followed artists (`/v2/illust/follow?restrict=all`).
     */
    suspend fun fetchFollowingLatestIllusts(
        accessToken: String,
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl ?: "$APP_API_HOST/v2/illust/follow?restrict=all"
                val json = getJson(endpoint, accessToken)
                val list = parseIllustArray(json.optJSONArray("illusts"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Fetches followed artists with their recent works (`/v1/user/following`).
     */
    suspend fun fetchFollowedArtistsStream(
        accessToken: String,
        userId: String,
        nextUrl: String? = null
    ): Result<Pair<List<ArtistCard>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/user/following?filter=for_android&user_id=$userId&restrict=public"
                val json = getJson(endpoint, accessToken)
                val artists = parseUserPreviews(json.optJSONArray("user_previews"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                artists to next
            }
        }

    /**
     * Fetches user's bookmarked/favorited illustrations (`/v1/user/bookmarks/illust`).
     */
    suspend fun fetchUserBookmarks(
        accessToken: String,
        userId: String,
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/user/bookmarks/illust?user_id=$userId&restrict=public"
                val json = getJson(endpoint, accessToken)
                val list = parseIllustArray(json.optJSONArray("illusts"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Adds or removes an illustration bookmark (`/v2/illust/bookmark/add` or `/v1/illust/bookmark/delete`).
     */
    suspend fun setBookmarkIllust(
        accessToken: String,
        illustId: Long,
        bookmark: Boolean,
        restrict: String = "public"
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = if (bookmark) "/v2/illust/bookmark/add" else "/v1/illust/bookmark/delete"
                val form = mutableMapOf("illust_id" to illustId.toString())
                if (bookmark) form["restrict"] = restrict
                postForm("$APP_API_HOST$path", form, accessToken)
                Unit
            }
        }

    /**
     * Fetches trending / recommended tags on Pixiv (`/v1/trending-tags/illust`).
     */
    suspend fun fetchTrendingTags(accessToken: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "$APP_API_HOST/v1/trending-tags/illust?filter=for_android"
                val json = getJson(endpoint, accessToken)
                val arr = json.optJSONArray("trend_tags") ?: JSONArray()
                val tags = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val tag = obj.optString("tag", "")
                    if (tag.isNotBlank()) {
                        tags.add("#$tag")
                    }
                }
                tags
            }
        }

    /**
     * Fetches autocomplete tag suggestions when typing >= 2 chars (`/v2/search/autocomplete`).
     */
    suspend fun fetchAutocompleteSuggestions(accessToken: String, word: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(word, "UTF-8")
                val endpoint = "$APP_API_HOST/v2/search/autocomplete?word=$encoded&merge_plain_keyword_results=true"
                val json = getJson(endpoint, accessToken)
                val tagsArr = json.optJSONArray("tags") ?: JSONArray()
                val out = mutableListOf<String>()
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.getJSONObject(i).optString("name")
                    if (t.isNotBlank()) out.add(t)
                }
                out
            }
        }

    /**
     * Fetches user comments on an illustration (`/v3/illust/comments`).
     */
    suspend fun fetchIllustComments(
        accessToken: String,
        illustId: Long,
        nextUrl: String? = null
    ): Result<Pair<List<IllustComment>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v3/illust/comments?illust_id=$illustId"
                val json = getJson(endpoint, accessToken)
                val arr = json.optJSONArray("comments") ?: JSONArray()
                val list = mutableListOf<IllustComment>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val user = obj.optJSONObject("user")
                    val profileUrls = user?.optJSONObject("profile_image_urls")
                    list.add(
                        IllustComment(
                            id = obj.optLong("id", i.toLong()),
                            userId = user?.optLong("id", 0L) ?: 0L,
                            userName = user?.optString("name", "Pixiv User") ?: "Pixiv User",
                            userAccount = "@${user?.optString("account", "user") ?: "user"}",
                            userAvatarUrl = profileUrls?.optString("medium"),
                            comment = obj.optString("comment", ""),
                            dateFormatted = obj.optString("date", "").take(10)
                        )
                    )
                }
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Fetches users who bookmarked/liked a specific illustration (`/v1/illust/bookmark/users` or fallback).
     */
    suspend fun fetchBookmarkUsers(
        accessToken: String,
        illustId: Long,
        nextUrl: String? = null
    ): Result<Pair<List<ArtistCard>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v1/illust/bookmark/users?filter=for_android&illust_id=$illustId"
                val json = getJson(endpoint, accessToken)
                val previews = json.optJSONArray("user_previews")
                val list = if (previews != null) {
                    parseUserPreviews(previews)
                } else {
                    val usersArr = json.optJSONArray("users") ?: JSONArray()
                    val out = mutableListOf<ArtistCard>()
                    for (i in 0 until usersArr.length()) {
                        val u = usersArr.getJSONObject(i)
                        val profileUrls = u.optJSONObject("profile_image_urls")
                        out.add(
                            ArtistCard(
                                id = u.optLong("id", i.toLong()),
                                name = u.optString("name", "Pixiv User"),
                                handle = "@${u.optString("account", "user")}",
                                avatarUrl = profileUrls?.optString("medium"),
                                isFollowed = u.optBoolean("is_followed", false)
                            )
                        )
                    }
                    out
                }
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Fetches Ugoira animation metadata (`/v1/ugoira/metadata`).
     */
    suspend fun fetchUgoiraMetadata(
        accessToken: String,
        illustId: Long
    ): Result<UgoiraMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = "$APP_API_HOST/v1/ugoira/metadata?illust_id=$illustId"
                val json = getJson(endpoint, accessToken)
                val meta = json.getJSONObject("ugoira_metadata")
                val zipUrls = meta.getJSONObject("zip_urls")
                val zipUrl = zipUrls.optString("medium")
                val framesArr = meta.getJSONArray("frames")
                val frames = mutableListOf<UgoiraFrame>()
                for (i in 0 until framesArr.length()) {
                    val f = framesArr.getJSONObject(i)
                    frames.add(
                        UgoiraFrame(
                            file = f.getString("file"),
                            delayMs = f.optInt("delay", 60).coerceAtLeast(16)
                        )
                    )
                }
                UgoiraMetadata(zipUrl = zipUrl, frames = frames)
            }
        }

    /**
     * Downloads Ugoira ZIP with Referer header and decodes frames into ImageBitmaps in memory.
     */
    suspend fun downloadAndDecodeUgoiraFrames(
        zipUrl: String,
        hostProxy: String = "i.pximg.net"
    ): Map<String, ImageBitmap> = withContext(Dispatchers.IO) {
        if (zipUrl.isBlank()) return@withContext emptyMap()
        val normalizedUrl = if (hostProxy.contains("pixiv.re")) {
            zipUrl.replace("i.pximg.net", "i.pixiv.re")
        } else {
            zipUrl
        }
        runCatching {
            val conn = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Referer", PXIMG_REFERER)
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@runCatching emptyMap()
            }
            val map = mutableMapOf<String, ImageBitmap>()
            ZipInputStream(BufferedInputStream(conn.inputStream)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val baos = ByteArrayOutputStream()
                        var len = zis.read(buffer)
                        while (len > 0) {
                            baos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }
                        val bytes = baos.toByteArray()
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        if (bmp != null) {
                            map[entry.name] = bmp
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    /**
     * Fetches recommended illustrations based on a post (`/v2/illust/related`).
     */
    suspend fun fetchRelatedIllusts(
        accessToken: String,
        illustId: Long,
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = nextUrl
                    ?: "$APP_API_HOST/v2/illust/related?filter=for_android&illust_id=$illustId"
                val json = getJson(endpoint, accessToken)
                val list = parseIllustArray(json.optJSONArray("illusts"))
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Searches illustrations or novels by keyword + comprehensive filters (`/v1/search/illust` or `/v1/search/novel`).
     */
    suspend fun searchIllusts(
        accessToken: String,
        word: String,
        filters: SearchFilters = SearchFilters(),
        nextUrl: String? = null
    ): Result<Pair<List<IllustItem>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val endpoint = if (!nextUrl.isNullOrBlank()) {
                    nextUrl
                } else {
                    val encoded = URLEncoder.encode(word.removePrefix("#"), "UTF-8")
                    val basePath = if (filters.workType == SearchWorkType.NOVELS) {
                        "$APP_API_HOST/v1/search/novel"
                    } else {
                        "$APP_API_HOST/v1/search/illust"
                    }
                    val sb = StringBuilder("$basePath?filter=for_android&word=$encoded&search_target=partial_match_for_tags&sort=${filters.sortOption.apiSort}")
                    if (!filters.showAiGenerated) {
                        sb.append("&search_ai_type=1")
                    }
                    val daysBack = filters.dateRange.daysBack
                    if (daysBack != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val cal = Calendar.getInstance()
                        val endDate = sdf.format(cal.time)
                        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
                        val startDate = sdf.format(cal.time)
                        sb.append("&start_date=$startDate&end_date=$endDate")
                    }
                    if (filters.likeFilter.minLikes > 0) {
                        sb.append("&bookmark_num_min=${filters.likeFilter.minLikes}")
                    }
                    sb.toString()
                }
                val json = getJson(endpoint, accessToken)
                val list = if (filters.workType == SearchWorkType.NOVELS) {
                    parseNovelArray(json.optJSONArray("novels"))
                } else {
                    parseIllustArray(json.optJSONArray("illusts"))
                }
                val next = json.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
                list to next
            }
        }

    /**
     * Searches artists by keyword (`/v1/search/user`).
     */
    suspend fun searchArtists(
        accessToken: String,
        word: String
    ): Result<List<ArtistCard>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(word.removePrefix("@"), "UTF-8")
                val endpoint = "$APP_API_HOST/v1/search/user?filter=for_android&word=$encoded"
                val json = getJson(endpoint, accessToken)
                parseUserPreviews(json.optJSONArray("user_previews"))
            }
        }

    /**
     * Downloads an image from `i.pximg.net` sending the required `Referer: https://app-api.pixiv.net/` header.
     */
    suspend fun loadPximgBitmap(imageUrl: String, hostProxy: String = "i.pximg.net"): ImageBitmap? {
        if (imageUrl.isBlank()) return null
        val normalizedUrl = if (hostProxy.contains("pixiv.re")) {
            imageUrl.replace("i.pximg.net", "i.pixiv.re")
        } else {
            imageUrl
        }
        bitmapCache.get(normalizedUrl)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Referer", PXIMG_REFERER)
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        if (bmp != null) {
                            bitmapCache.put(normalizedUrl, bmp)
                        }
                        bmp
                    }
                } else null
            }.getOrNull()
        }
    }

    private fun parseIllustArray(arr: JSONArray?, ranked: Boolean = false, rankOffset: Int = 0): List<IllustItem> {
        if (arr == null) return emptyList()
        val list = mutableListOf<IllustItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optLong("id", i.toLong())
            val title = obj.optString("title", "Untitled")
            val userObj = obj.optJSONObject("user")
            val artistId = userObj?.optLong("id") ?: 0L
            val artistName = userObj?.optString("name") ?: "Pixiv Artist"
            val artistAccount = userObj?.optString("account") ?: "artist"
            val artistProfileUrls = userObj?.optJSONObject("profile_image_urls")
            val artistAvatarUrl = artistProfileUrls?.optString("medium")
            val isArtistFollowed = userObj?.optBoolean("is_followed", false) ?: false

            val totalBookmarks = obj.optInt("total_bookmarks", 0)
            val totalView = obj.optInt("total_view", 0)
            val width = obj.optInt("width", 3).coerceAtLeast(1)
            val height = obj.optInt("height", 4).coerceAtLeast(1)
            val ratio = (width.toFloat() / height.toFloat()).coerceIn(0.62f, 1.35f)

            val imageUrls = obj.optJSONObject("image_urls")
            val squareMediumUrl = imageUrls?.optString("square_medium")
            val mediumUrl = imageUrls?.optString("medium") ?: squareMediumUrl
            val largeUrl = imageUrls?.optString("large") ?: mediumUrl

            val metaSingle = obj.optJSONObject("meta_single_page")
            var originalUrl = metaSingle?.optString("original_image_url")?.takeIf { it.isNotBlank() }

            val metaPagesArr = obj.optJSONArray("meta_pages")
            val pageUrls = mutableListOf<String>()
            if (metaPagesArr != null && metaPagesArr.length() > 0) {
                for (p in 0 until metaPagesArr.length()) {
                    val pageImgObj = metaPagesArr.getJSONObject(p).optJSONObject("image_urls")
                    val pLarge = pageImgObj?.optString("large") ?: pageImgObj?.optString("medium")
                    val pOrig = pageImgObj?.optString("original") ?: pLarge
                    if (!pLarge.isNullOrBlank()) pageUrls.add(pLarge)
                    if (p == 0 && originalUrl == null) originalUrl = pOrig
                }
            }
            if (originalUrl == null) originalUrl = largeUrl

            val tagsArr = obj.optJSONArray("tags")
            val tags = mutableListOf<IllustTag>()
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(t)
                    val tagName = tagObj.optString("name")
                    val translated = tagObj.optString("translated_name")
                        .takeIf { it.isNotBlank() && it != "null" }
                    if (tagName.isNotBlank()) {
                        tags.add(IllustTag(name = tagName, translatedName = translated))
                    }
                }
            }

            val aiType = obj.optInt("ai_type", 0)
            val xRestrict = obj.optInt("x_restrict", 0)
            val createDate = obj.optString("create_date", "").take(10)

            list.add(
                IllustItem(
                    id = id,
                    title = title,
                    type = obj.optString("type", "illust"),
                    artistId = artistId,
                    artistName = artistName,
                    artistHandle = "@$artistAccount",
                    artistAvatarUrl = artistAvatarUrl,
                    isArtistFollowed = isArtistFollowed,
                    likesFormatted = formatCompactNumber(totalBookmarks),
                    bookmarkCount = totalBookmarks,
                    totalViewCount = totalView,
                    aspectRatio = ratio,
                    tags = tags,
                    isBookmarked = obj.optBoolean("is_bookmarked", false),
                    isAiGenerated = aiType == 2,
                    isR18 = xRestrict >= 1,
                    isR18G = xRestrict == 2,
                    rankNumber = if (ranked) rankOffset + i + 1 else null,
                    imageUrlSquareMedium = squareMediumUrl,
                    imageUrlMedium = mediumUrl,
                    imageUrlLarge = largeUrl,
                    imageUrlOriginal = originalUrl,
                    metaPageUrls = pageUrls,
                    caption = cleanHtml(obj.optString("caption", "")),
                    createDateFormatted = createDate,
                    pageCount = obj.optInt("page_count", 1),
                    seriesId = obj.optJSONObject("series")?.optLong("id")?.takeIf { it > 0L },
                    seriesTitle = obj.optJSONObject("series")?.optString("title")?.takeIf { it.isNotBlank() && it != "null" }
                )
            )
        }
        return list
    }

    private fun parseNovelArray(arr: JSONArray?): List<IllustItem> {
        if (arr == null) return emptyList()
        val list = mutableListOf<IllustItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optLong("id", i.toLong())
            val title = obj.optString("title", "Untitled Novel")
            val userObj = obj.optJSONObject("user")
            val artistId = userObj?.optLong("id") ?: 0L
            val artistName = userObj?.optString("name") ?: "Pixiv Author"
            val artistAccount = userObj?.optString("account") ?: "author"
            val artistAvatarUrl = userObj?.optJSONObject("profile_image_urls")?.optString("medium")
            val totalBookmarks = obj.optInt("total_bookmarks", 0)
            val imageUrls = obj.optJSONObject("image_urls")
            val coverUrl = imageUrls?.optString("large") ?: imageUrls?.optString("medium")
            val tagsArr = obj.optJSONArray("tags")
            val tags = mutableListOf<IllustTag>()
            if (tagsArr != null) {
                for (t in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(t)
                    val tagName = tagObj.optString("name")
                    val translated = tagObj.optString("translated_name")
                        .takeIf { it.isNotBlank() && it != "null" }
                    if (tagName.isNotBlank()) {
                        tags.add(IllustTag(name = tagName, translatedName = translated))
                    }
                }
            }
            val aiType = obj.optInt("ai_type", 0)
            val xRestrict = obj.optInt("x_restrict", 0)
            list.add(
                IllustItem(
                    id = id,
                    title = title,
                    type = "novel",
                    artistId = artistId,
                    artistName = artistName,
                    artistHandle = "@$artistAccount",
                    artistAvatarUrl = artistAvatarUrl,
                    isArtistFollowed = userObj?.optBoolean("is_followed", false) ?: false,
                    likesFormatted = formatCompactNumber(totalBookmarks),
                    bookmarkCount = totalBookmarks,
                    aspectRatio = 0.72f,
                    tags = tags,
                    isBookmarked = obj.optBoolean("is_bookmarked", false),
                    isAiGenerated = aiType == 2,
                    isR18 = xRestrict >= 1,
                    isR18G = xRestrict == 2,
                    imageUrlSquareMedium = coverUrl,
                    imageUrlMedium = coverUrl,
                    imageUrlLarge = coverUrl,
                    imageUrlOriginal = coverUrl,
                    caption = cleanHtml(obj.optString("caption", "")),
                    createDateFormatted = obj.optString("create_date", "").take(10),
                    pageCount = obj.optInt("page_count", 1)
                )
            )
        }
        return list
    }

    private fun cleanHtml(raw: String): String {
        if (raw.isBlank()) return ""
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }

    private fun parseUserPreviews(arr: JSONArray?): List<ArtistCard> {
        if (arr == null) return emptyList()
        val out = mutableListOf<ArtistCard>()
        for (i in 0 until arr.length()) {
            val preview = arr.getJSONObject(i)
            val user = preview.optJSONObject("user") ?: continue
            val userId = user.optLong("id", i.toLong())
            val name = user.optString("name", "Artist")
            val account = user.optString("account", "creator")
            val profileUrls = user.optJSONObject("profile_image_urls")
            val avatarUrl = profileUrls?.optString("medium")
            val isFollowed = user.optBoolean("is_followed", false)
            val illusts = parseIllustArray(preview.optJSONArray("illusts")).take(5)
            val latestDate = illusts.firstOrNull()?.createDateFormatted ?: ""
            out.add(
                ArtistCard(
                    id = userId,
                    name = name,
                    handle = "@$account",
                    avatarUrl = avatarUrl,
                    isFollowed = isFollowed,
                    followersFormatted = "@$account",
                    latestPostDate = latestDate,
                    bio = user.optString("comment", ""),
                    recentIllusts = illusts
                )
            )
        }
        return out
    }

    private fun getJson(urlStr: String, accessToken: String?): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            generateClientHeaders().forEach { (k, v) -> setRequestProperty(k, v) }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = if (stream != null) BufferedReader(InputStreamReader(stream)).use { it.readText() } else ""
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code: $text")
        }
        return JSONObject(text)
    }

    private fun postForm(urlStr: String, form: Map<String, String>, accessToken: String?): JSONObject {
        val body = form.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            generateClientHeaders().forEach { (k, v) -> setRequestProperty(k, v) }
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = if (stream != null) BufferedReader(InputStreamReader(stream)).use { it.readText() } else ""
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code: $text")
        }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    suspend fun downloadPximgBytes(url: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 20000
                    readTimeout = 60000
                    setRequestProperty("Referer", "https://app-api.pixiv.net/")
                    setRequestProperty("User-Agent", "PixivAndroidApp/5.0.234 (Android 14; Pixel 8)")
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code while downloading media")
                }
                conn.inputStream.use { it.readBytes() }
            }
        }

    fun formatCompactNumber(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
            count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
            else -> count.toString()
        }
    }
}
