package com.lagradost.quicknovel.providers

import android.annotation.SuppressLint
import com.lagradost.quicknovel.*
import org.jsoup.Jsoup
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class RoyalRoadProvider : MainAPI() {
    override val name: String get() = "Royal Road"
    override val mainUrl: String get() = "https://www.royalroad.com"

    override val hasMainPage: Boolean
        get() = true

    override val iconId: Int
        get() = R.drawable.big_icon_royalroad

    override val iconBackgroundId: Int
        get() = R.color.royalRoadColor

    override val orderBys: List<Pair<String, String>>
        get() = listOf(
            Pair("Best Rated", "best-rated"),
            Pair("Ongoing", "active-popular"),
            Pair("Completed", "complete"),
            Pair("Popular this week", "weekly-popular"),
            Pair("Latest Updates", "latest-updates"),
            Pair("New Releases", "new-releases"),
            Pair("Trending", "trending"),
        )
    override val tags: List<Pair<String, String>>
        get() = listOf(
            Pair("All", ""),
            Pair("Action", "action"),
            Pair("Adventure", "adventure"),
            Pair("Comedy", "comedy"),
            Pair("Contemporary", "contemporary"),
            Pair("Drama", "drama"),
            Pair("Fantasy", "fantasy"),
            Pair("Historical", "historical"),
            Pair("Horror", "horror"),
            Pair("Mystery", "mystery"),
            Pair("Psychological", "psychological"),
            Pair("Romance", "romance"),
            Pair("Satire", "satire"),
            Pair("Sci-fi", "sci_fi"),
            Pair("LitRPG", "litrpg"),
            Pair("Magic", "magic"),
            Pair("GameLit", "gamelit"),
            Pair("Male Lead", "male_lead"),
            Pair("Female Lead", "female_lead"),
            Pair("Portal Fantasy / Isekai", "summoned_hero"),
            Pair("Reincarnation", "reincarnation"),
            Pair("High Fantasy", "high_fantasy"),
            Pair("Harem", "harem"),
            Pair("Gender Bender", "gender_bender"),
            Pair("Anti-Hero Lead", "anti-hero_lead"),
            Pair("Progression", "Progression"),
            Pair("Strategy", "strategy"),
            Pair("Short Story", "one_shot"),
            Pair("Tragedy", "tragedy")
        )

    override val hasReviews: Boolean
        get() = true

    @SuppressLint("SimpleDateFormat")
    override fun loadReviews(url: String, page: Int, showSpoilers: Boolean): ArrayList<UserReview> {
        val realUrl = "$url?sorting=top&reviews=$page" //SORTING ??
        val response = khttp.get(realUrl)

        val document = Jsoup.parse(response.text)
        val reviews = document.select("div.reviews-container > div.review")
        if (reviews.size <= 0) return ArrayList()
        val returnValue: ArrayList<UserReview> = ArrayList()
        for (r in reviews) {
            val textContent = r.selectFirst("> div.review-right-content")
            val scoreContent = r.selectFirst("> div.review-side")
            fun parseScore(data: String): Int {
                return ((data.replace("stars", "")
                    .toFloatOrNull() ?: -1f) * 200).toInt()
            }

            val scoreHeader = scoreContent.selectFirst("> div.scores > div")
            var overallScore =
                parseScore(
                    scoreHeader.selectFirst("> div.overall-score-container")
                        .select("> div")[1].attr("aria-label")
                )

            if (overallScore < 0) { //SOMETHING WENT WRONG
                val divHeader = scoreHeader.selectFirst("> div.overall-score-container")
                val divs = divHeader
                    .select("> div")
                val names = divs[1].selectFirst("> div")

                overallScore = 20 * when { // PROBS FOR LOOP HERE
                    names.hasClass("star-50") -> {
                        50
                    }
                    names.hasClass("star-45") -> {
                        45
                    }
                    names.hasClass("star-40") -> {
                        40
                    }
                    names.hasClass("star-35") -> {
                        35
                    }
                    names.hasClass("star-30") -> {
                        30
                    }
                    names.hasClass("star-25") -> {
                        25
                    }
                    names.hasClass("star-20") -> {
                        20
                    }
                    names.hasClass("star-15") -> {
                        15
                    }
                    names.hasClass("star-10") -> {
                        10
                    }
                    names.hasClass("star-5") -> {
                        5
                    }
                    else -> {
                        -10
                    }
                }
            }
            if (overallScore < 0) continue // JUST IN CASE

            val avatar = scoreContent.selectFirst("> div.avatar-container-general > img")
            val avatarUrl = avatar.attr("src")

            val scores = scoreHeader.select("> div.advanced-score")
            val scoresData = if (scores.size <= 0) ArrayList<Pair<Int, String>>() else scores.map { s ->
                val divs = s.select("> div")
                Pair(parseScore(divs[1].attr("aria-label")), divs[0].text())
            }

            val reviewHeader = textContent.selectFirst("> div.review-header")
            val reviewMeta = reviewHeader.selectFirst("> div.review-meta")

            val reviewTitle = reviewHeader.selectFirst("> div > div > h4").text()

            val username = reviewMeta.selectFirst("> span > a").text()

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
            val date = Date(reviewMeta.selectFirst("> span > a > time").attr("unixtime").toLong() * 1000)

            val reviewTime = sdf.format(date).toString()

            val reviewContent = textContent.selectFirst("> div.review-content")
            if (!showSpoilers) reviewContent.removeClass("spoiler")
            val reviewTxt = reviewContent.text()

            returnValue.add(
                UserReview(
                    reviewTxt,
                    reviewTitle,
                    username,
                    reviewTime,
                    fixUrl(avatarUrl),
                    overallScore,
                    ArrayList(scoresData)
                )
            )
        }
        return returnValue

    }

    override fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?,
    ): HeadMainPageResponse {
        val url = "$mainUrl/fictions/$orderBy?page=$page${if (tag == null || tag == "") "" else "&genre=$tag"}"
        if (page > 1 && orderBy == "trending") return HeadMainPageResponse(
            url,
            ArrayList()
        ) // TRENDING ONLY HAS 1 PAGE

        val response = khttp.get(url)

        val document = Jsoup.parse(response.text)
        val headers = document.select("div.fiction-list-item")
        if (headers.size <= 0) return HeadMainPageResponse(url, ArrayList())

        val returnValue: ArrayList<SearchResponse> = ArrayList()
        for (h in headers) {
            val head = h.selectFirst("> div")
            val hInfo = head.selectFirst("> h2.fiction-title > a")

            val name = hInfo.text()
            val cUrl = mainUrl + hInfo.attr("href")

            val posterUrl = h.selectFirst("> figure > a > img").attr("src")

            val rating = try {
                val ratingHead =
                    head.selectFirst("> div.stats").select("> div")[1].selectFirst("> span").attr("title")
                (ratingHead.toFloat() * 200).toInt()
            } catch (e: Exception) {
                null
            }

            val latestChapter = try {
                if (orderBy == "latest-updates") {
                    head.selectFirst("> ul.list-unstyled > li.list-item > a > span").text()
                } else {
                    h.select("div.stats > div.col-sm-6 > span")[4].text()
                }
            } catch (e: Exception) {
                null
            }

            //val tags = ArrayList(h.select("span.tags > a").map { t -> t.text() })
            returnValue.add(
                SearchResponse(
                    name,
                    fixUrl(cUrl),
                    fixUrl(posterUrl),
                    rating,
                    latestChapter,
                    this.name
                )
            )
            //tags))
        }
        return HeadMainPageResponse(url, returnValue)
    }


    override fun search(query: String): List<SearchResponse> {
        val response = khttp.get("$mainUrl/fictions/search?title=$query")

        val document = Jsoup.parse(response.text)
        val headers = document.select("div.fiction-list-item")
        if (headers.size <= 0) return ArrayList()
        val returnValue: ArrayList<SearchResponse> = ArrayList()
        for (h in headers) {
            val head = h.selectFirst("> div.search-content")
            val hInfo = head.selectFirst("> h2.fiction-title > a")

            val name = hInfo.text()
            val url = mainUrl + hInfo.attr("href")

            val posterUrl = h.selectFirst("> figure.text-center > a > img").attr("src")

            val ratingHead = head.selectFirst("> div.stats").select("> div")[1].selectFirst("> span").attr("title")
            val rating = (ratingHead.toFloat() * 200).toInt()
            val latestChapter = h.select("div.stats > div.col-sm-6 > span")[4].text()
            returnValue.add(SearchResponse(name, url, fixUrl(posterUrl), rating, latestChapter, this.name))
        }
        return returnValue
    }

    override fun load(url: String): LoadResponse {
        val response = khttp.get(url)

        val document = Jsoup.parse(response.text)

        val ratingAttr = document.selectFirst("span.font-red-sunglo").attr("data-content")
        val rating = (ratingAttr.substring(0, ratingAttr.indexOf('/')).toFloat() * 200).toInt()
        val name = document.selectFirst("h1.font-white").text()
        val author = document.selectFirst("h4.font-white > span > a").text()
        val tagsDoc = document.select("span.tags > a")
        val tags: ArrayList<String> = ArrayList()
        for (t in tagsDoc) {
            tags.add(t.text())
        }

        var synopsis = ""
        val synoDescript = document.select("div.description > div")
        val synoParts = synoDescript.select("> p")
        if (synoParts.size == 0 && synoDescript.hasText()) {
            synopsis = synoDescript.text().replace("\n", "\n\n") // JUST IN CASE
        } else {
            for (s in synoParts) {
                synopsis += s.text()!! + "\n\n"
            }
        }

        val data: ArrayList<ChapterData> = ArrayList()
        val chapterHeaders = document.select("div.portlet-body > table > tbody > tr")
        for (c in chapterHeaders) {
            val cUrl = c.attr("data-url")
            val td = c.select("> td") // 0 = Name, 1 = Upload
            val cName = td[0].selectFirst("> a").text()
            val added = td[1].selectFirst("> a > time").text()
            val views = null
            data.add(ChapterData(cName, fixUrl(cUrl), added, views))
        }
        val posterUrl = document.selectFirst("div.fic-header > div > img").attr("src")

        val hStates = document.select("ul.list-unstyled")[1]
        val stats = hStates.select("> li")
        val views = stats[1].text().replace(",", "").replace(".", "").toInt()
        val peopleRatedHeader = document.select("div.stats-content > div > meta")
        val peopleRated = peopleRatedHeader[2].attr("content").toInt()

        val statusTxt = document.select("div.col-md-8 > div.margin-bottom-10 > span.label")

        var status = 0
        for (s in statusTxt) {
            if (s.hasText()) {
                status = when (s.text()) {
                    "ONGOING" -> STATUS_ONGOING
                    "COMPLETED" -> STATUS_COMPLETE
                    "HIATUS" -> STATUS_PAUSE
                    "STUB" -> STATUS_DROPPED
                    "DROPPED" -> STATUS_DROPPED
                    else -> STATUS_NULL
                }
                if (status > 0) break
            }
        }

        return LoadResponse(
            url,
            name,
            data,
            author,
            fixUrl(posterUrl),
            rating,
            peopleRated,
            views,
            synopsis,
            tags,
            status
        )
    }

    override fun loadHtml(url: String): String? {
        val response = khttp.get(url)
        val document = Jsoup.parse(response.text)
        return document.selectFirst("div.chapter-content").html()
    }
}