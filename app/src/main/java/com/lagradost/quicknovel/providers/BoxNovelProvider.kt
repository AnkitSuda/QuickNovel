package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.*
import org.jsoup.Jsoup
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class BoxNovelProvider : MainAPI() {
    override val name: String
        get() = "BoxNovel"
    override val mainUrl: String
        get() = "https://boxnovel.com"
    override val iconId: Int
        get() = R.drawable.big_icon_boxnovel

    override val hasMainPage: Boolean
        get() = true

    override val iconBackgroundId: Int
        get() = R.color.boxNovelColor

    override val tags: List<Pair<String, String>>
        get() = listOf(
            Pair("All", ""),
            Pair("Completed", "completed"),
            Pair("Action", "action"),
            Pair("Adventure", "adventure"),
            Pair("Comedy", "comedy"),
            Pair("Drama", "genre"),
            Pair("Ecchi", "ecchi"),
            Pair("Fantasy", "fantasy"),
            Pair("Harem", "harem"),
            Pair("Josei", "josei"),
            Pair("Martial Arts", "martial-arts"),
            Pair("Gender Bender", "gender-bender"),
            Pair("Historical", "historical"),
            Pair("Horror", "horror"),
            Pair("Mature", "mature"),
            Pair("Mecha", "mecha"),
            Pair("Mystery", "mystery"),
            Pair("Psychological", "psychological"),
            Pair("Romance", "romance"),
            Pair("School Life", "school-life"),
            Pair("Sci-fi", "sci-fi"),
            Pair("Seinen", "seinen"),
            Pair("Shoujo", "shoujo"),
            Pair("Shounen", "shounen"),
            Pair("Slice of Life", "slice-of-life"),
            Pair("Sports", "sports"),
            Pair("Supernatural", "supernatural"),
            Pair("Tragedy", "tragedy"),
            Pair("Wuxia", "wuxia"),
            Pair("Xianxia", "xianxia"),
            Pair("Xuanhuan", "xuanhuan"),
        )

    override val orderBys: List<Pair<String, String>>
        get() = listOf(
            Pair("Nothing", ""),
            Pair("New", "new-manga"),
            Pair("Most Views", "views"),
            Pair("Trending", "trending"),
            Pair("Rating", "rating"),
            Pair("A-Z", "alphabet"),
            Pair("Latest", "latest"),
        )

    override fun loadMainPage(
        page: Int,
        mainCategory: String?,
        orderBy: String?,
        tag: String?,
    ): HeadMainPageResponse {

        val order = when (tag) {
            "" -> "novel"
            null -> "novel"
            "completed" -> "manga-tag/$tag"
            else -> "manga-genre/$tag"
        }
        val url = "$mainUrl/$order/page/$page/${if (orderBy == null || orderBy == "") "" else "?m_orderby=$orderBy"}"

        val response = khttp.get(url)

        val document = Jsoup.parse(response.text)
        //""div.page-content-listing > div.page-listing-item > div > div > div.page-item-detail"
        val headers = document.select("div.page-item-detail")
        if (headers.size <= 0) return HeadMainPageResponse(url, ArrayList())

        val returnValue: ArrayList<SearchResponse> = ArrayList()
        for (h in headers) {
            val imageHeader = h.selectFirst("div.item-thumb > a")
            val name = imageHeader.attr("title")
            if (name.contains("Comic")) continue // I DON'T WANT MANGA!

            val cUrl = imageHeader.attr("href")
            val posterUrl = imageHeader.selectFirst("> img").attr("src")
            val sum = h.selectFirst("div.item-summary")
            val rating =
                (sum.selectFirst("> div.rating > div.post-total-rating > span.score").text()
                    .toFloat() * 200).toInt()
            val latestChap = sum.selectFirst("> div.list-chapter > div.chapter-item > span > a").text()
            returnValue.add(SearchResponse(name, cUrl, posterUrl, rating, latestChap, this.name))
        }

        return HeadMainPageResponse(url, returnValue)
    }

    override fun loadHtml(url: String): String? {
        val response = khttp.get(url)
        val document = Jsoup.parse(response.text)
        val res = document.selectFirst("div.text-left")
        if (res.html() == "") {
            return null
        }
        return res.html()
            .replace("(adsbygoogle = window.adsbygoogle || []).push({});", "")
            .replace(
                "Read latest Chapters at BoxNovel.Com Only",
                ""
            ) // HAVE NOT TESTED THIS ONE, COPY FROM WUXIAWORLD

    }

    override fun search(query: String): List<SearchResponse> {
        val response = khttp.get("$mainUrl/?s=$query&post_type=wp-manga")

        val document = Jsoup.parse(response.text)
        val headers = document.select("div.c-tabs-item__content")
        if (headers.size <= 0) return ArrayList()
        val returnValue: ArrayList<SearchResponse> = ArrayList()
        for (h in headers) {
            val head = h.selectFirst("> div > div.tab-summary")
            val title = head.selectFirst("> div.post-title > h3 > a")
            val name = title.text()

            if (name.contains("Comic")) continue // I DON'T WANT MANGA!

            val url = title.attr("href")

            val posterUrl = h.selectFirst("> div > div.tab-thumb > a > img").attr("src")

            val meta = h.selectFirst("> div > div.tab-meta")

            val ratingTxt = meta.selectFirst("> div.rating > div.post-total-rating > span.total_votes").text()

            val rating = if (ratingTxt != null) {
                (ratingTxt.toFloat() * 200).toInt()
            } else {
                null
            }

            val latestChapter = meta.selectFirst("> div.latest-chap > span.chapter > a").text()
            returnValue.add(SearchResponse(name, url, posterUrl, rating, latestChapter, this.name))
        }
        return returnValue
    }

    override fun load(url: String): LoadResponse {
        val response = khttp.get(url)

        val document = Jsoup.parse(response.text)
        println(response.text)
        val name = document.selectFirst("div.post-title > h1").text().replace("  ", " ").replace("\n", "")
            .replace("\t", "")
        val authors = document.select("div.author-content > a")
        var author = ""
        for (a in authors) {
            val atter = a.attr("href")
            if (atter.length > "$mainUrl/manga-author/".length && atter.startsWith("$mainUrl/manga-author/")) {
                author = a.text()
                break
            }
        }

        val posterUrl = document.select("div.summary_image > a > img").attr("src")

        val tags: ArrayList<String> = ArrayList()
        val tagsHeader = document.select("div.genres-content > a")
        for (t in tagsHeader) {
            tags.add(t.text())
        }

        var synopsis = ""
        var synoParts = document.select("#editdescription > p")
        if (synoParts.size == 0) synoParts = document.select("div.j_synopsis > p")
        if (synoParts.size == 0) synoParts = document.select("div.summary__content > p")
        for (s in synoParts) {
            if (s.hasText() && !s.text().toLowerCase(Locale.getDefault()).contains(mainUrl)) { // FUCK ADS
                synopsis += s.text()!! + "\n\n"
            }
        }

        //val id = WuxiaWorldSiteProvider.getId(response.text) ?: throw ErrorLoadingException("No id found")
        //ajax/chapters/
        val chapResponse = khttp.post(
            "${url}ajax/chapters/",
        )
        val data = WuxiaWorldSiteProvider.getChapters(chapResponse.text)

        val rating = ((document.selectFirst("span#averagerate")?.text()?.toFloat() ?: 0f) * 200).toInt()
        val peopleVoted = document.selectFirst("span#countrate")?.text()?.toInt() ?: 0

        val views = null

        val aHeaders = document.select("div.post-status > div.post-content_item > div.summary-content")
        val aHeader = aHeaders.last()

        val status = when (aHeader.text().toLowerCase(Locale.getDefault())) {
            "ongoing" -> STATUS_ONGOING
            "completed" -> STATUS_COMPLETE
            else -> STATUS_NULL
        }

        return LoadResponse(url, name, data, author, posterUrl, rating, peopleVoted, views, synopsis, tags, status)
    }
}