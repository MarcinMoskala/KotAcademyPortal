@file:Suppress("IllegalIdentifier")

package com.marcinmoskala.kotlinacademy

import com.marcinmoskala.kotlinacademy.common.Cancellable
import com.marcinmoskala.kotlinacademy.common.UI
import com.marcinmoskala.kotlinacademy.common.launchUI
import com.marcinmoskala.kotlinacademy.data.News
import com.marcinmoskala.kotlinacademy.data.NewsData
import com.marcinmoskala.kotlinacademy.presentation.news.NewsPresenter
import com.marcinmoskala.kotlinacademy.presentation.news.NewsView
import com.marcinmoskala.kotlinacademy.respositories.NewsRepository
import com.marcinmoskala.kotlinacademy.usecases.PeriodicCaller
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NewsPresenterUnitTest {

    @Before
    fun setUp() {
        UI = Unconfined
        overrideNewsRepository({ NewsData(emptyList()) })
        overridePeriodicCaller({ _, _ -> object : Cancellable {} })
    }

    @Test
    fun `Is using PeriodicCaller to refresh list since onCreate`() {
        val view = NewsView()
        var periodicCallerStarts: List<Long> = listOf()
        overridePeriodicCaller { timeMillis, callback ->
            periodicCallerStarts += timeMillis
            Cancellable()
        }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        // Then
        assertEquals(1, periodicCallerStarts.size)
        assertEquals(NewsPresenter.AUTO_REFRESH_TIME_MS, periodicCallerStarts[0])
    }

    @Test
    fun `When onCreate, loads and displays list of news`() {
        val view = NewsView()
        val newsList = listOf(FAKE_NEWS)
        overrideNewsRepository { NewsData(newsList) }
        val presenter = NewsPresenter(view)
        // When
        presenter.onCreate()
        // Then
        assertEquals(newsList, view.newsList)
        assertEquals(0, view.displayedErrors.size)
    }

    @Test
    fun `When onCreate, loader is displayed during repository usage but not before and after onCreate operation`() {
        val view = NewsView()
        val newsList = listOf(FAKE_NEWS)
        var repositoryUsed = false
        overrideNewsRepository {
            assertTrue(view.loading)
            repositoryUsed = true
            NewsData(newsList)
        }
        val presenter = NewsPresenter(view)
        assertFalse(view.loading)
        // When
        presenter.onCreate()
        // Then
        assertFalse(view.loading)
        assertEquals(newsList, view.newsList)
        assertEquals(0, view.displayedErrors.size)
    }

    private fun overrideNewsRepository(getNewsData: () -> NewsData) {
        NewsRepository.override = object : NewsRepository {
            suspend override fun getNewsData(): NewsData = getNewsData()
        }
    }

    private fun overridePeriodicCaller(start: (timeMillis: Long, callback: () -> Unit) -> Cancellable) {
        PeriodicCaller.override = object : PeriodicCaller {
            override fun start(timeMillis: Long, callback: () -> Unit) = start(timeMillis, callback)
        }
    }

    private fun NewsView() = object : NewsView {
        override var loading: Boolean = false
        override var swipeRefresh: Boolean = false
        var newsList: List<News>? = null
        var displayedErrors: List<Throwable> = emptyList()

        override fun showList(news: List<News>) {
            newsList = news
        }

        override fun showError(throwable: Throwable) {
            displayedErrors += throwable
        }
    }

    private fun Cancellable() = object : Cancellable {}

    companion object {
        val FAKE_NEWS = News(1, "Some title", "Description", "Image url", "Url")
    }
}
