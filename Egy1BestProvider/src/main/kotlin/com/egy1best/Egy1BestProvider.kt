package com.egy1best

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Egy1BestProvider : MainAPI() {
    override var mainUrl = "https://egy1best.cimawbas.tv"
    override var name = "Egy1Best"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override var lang = "ar"
    
    override val hasMainPage = true

    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("movie", ignoreCase = true)) TvType.Movie else TvType.TvSeries
        }

        fun getYear(pattern: String): Int? {
            val yearRegex = Regex("\\d{4}")
            return yearRegex.find(pattern)?.value?.toIntOrNull()
        }

        fun Element.toSearchResponse(): SearchResponse? {
            val title = selectFirst("div > h3")?.text()?.trim() ?: return null
            val href = selectFirst("a")?.attr("href") ?: return null
            val poster = selectFirst("img")?.attr("data-src") ?: selectFirst("img")?.attr("src") ?: ""
            val quality = selectFirst("span.quality")?.text()?.trim()

            return newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                addQuality(quality)
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        
        val homeItems = mutableListOf<HomePageList>()
        
        // Popular Movies Section
        document.select(".section-block").forEach { section ->
            val sectionTitle = section.selectFirst("h2")?.text()?.trim() ?: "Movies"
            val items = section.select("article.item > div > div > article").mapNotNull { element ->
                element.toSearchResponse()
            }.take(20)
            
            if(items.isNotEmpty()) {
                homeItems.add(HomePageList(sectionTitle, items))
            }
        }
        
        return HomePageResponse(homeItems)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search/${query.replace(" ", "+")}"
        val document = app.get(searchUrl).document
        
        return document.select("article.item > div > div > article").mapNotNull { element ->
            element.toSearchResponse()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("h1.title")?.text()?.trim() 
            ?: document.selectFirst("div > h1")?.text()?.trim() 
            ?: document.selectFirst("title")?.text()?.substringBefore("-").trim() ?: ""
        
        val poster = document.selectFirst("div.poster > img")?.attr("data-src")
            ?: document.selectFirst("div.poster > img")?.attr("src")
            ?: document.selectFirst("meta[property=\"og:image\"]")?.attr("content")
        
        val description = document.selectFirst("div.desc > p")?.text()?.trim()
            ?: document.selectFirst("div.post-entry > p")?.text()?.trim()
        
        val year = document.selectFirst("div.meta > span.date")?.text()?.let { getYear(it) }
        
        val tags = document.select("div.meta > span.genre > a").map { it.text().trim() }
        
        val rating = document.selectFirst("div.rating > strong")?.text()?.replace("/10", "")?.toFloatOrNull()?.times(10)?.toInt()
        
        val type = if (url.contains("/series/", ignoreCase = true) || 
                      document.selectFirst("ul.episodes-list, div.seasons-wrap") != null) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }
        
        return if (type == TvType.TvSeries) {
            // Handle TV Series
            val episodes = document.select("ul.episodes-list li, div.season-block li").map { ep ->
                val epTitle = ep.selectFirst("a > span")?.text()?.trim() ?: "Episode ${ep.attr("data-number")}"
                val epLink = fixUrl(ep.selectFirst("a")?.attr("href") ?: "")
                val epNum = ep.attr("data-number").toIntOrNull()
                
                Episode(
                    data = epLink,
                    name = epTitle,
                    episode = epNum
                )
            }
            
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
            }
        } else {
            // Handle Movie
            val links = document.select("div.servers-list button[data-server]").map { 
                fixUrl(it.attr("data-link"))
            }
            
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        currQuality: String?,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Extract video links from iframe or direct sources
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank()) {
                loadExtractor(src, callback)
            }
        }
        
        // Look for direct video elements
        document.select("video source").forEach { source ->
            val src = source.attr("src")
            if (src.isNotBlank()) {
                callback.invoke(
                    ExtractorLink(
                        "Egy1Best",
                        "Egy1Best",
                        src,
                        "",
                        Qualities.Unknown.value,
                        false
                    )
                )
            }
        }
        
        return true
    }
}