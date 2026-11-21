package demo.e2e

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * Smoke checks for the demo page: the shell comes from the kit, the grid renders through the wrapper,
 * its buttons drive the grid API, and the language switch relabels the page without recreating the
 * grid.
 *
 * The shell lives in a shadow root; Playwright's CSS pierces shadow boundaries, `document.querySelector`
 * does not — so the waits use locators instead of `waitForFunction`.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `page renders the shell, the logo and an overview`(page: Page) {
        openHome(page)

        assertTrue(page.title().contains("ktjs-aggrid"), "title: ${page.title()}")
        assertTrue(page.locator("img.layout__logo").isVisible(), "the shell shows the project logo")
        assertEquals("Overview", page.locator("aggrid-overview-page h1").textContent())
        assertTrue(
            page.locator("vaadin-side-nav-item[data-route='grid']").isVisible(),
            "the sidebar lists the grid route",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `the grid renders five rows of the paged sample data`(page: Page) {
        openHome(page)
        goToGrid(page)

        // Five rows per page, as configured through `paginationPageSize`.
        assertEquals(5, page.locator(".ag-row").count(), "five rows on the first page")
        assertEquals("7", page.locator("[data-status]").getAttribute("data-rows"), "seven sample rows")
        assertEquals("2", page.locator("[data-status]").getAttribute("data-pages"), "two pages")
        assertTrue(
            (page.locator("[data-grid]").first().textContent() ?: "").contains("Make"),
            "the column headers come from the page's strings",
        )
    }

    @RetryingTest(maxAttempts = 2)
    fun `the toolbar adds and removes a row through the API`(page: Page) {
        openHome(page)
        goToGrid(page)

        page.locator("[data-action=add]").click()
        assertEquals("8", page.locator("[data-status]").getAttribute("data-rows"), "an eighth row")

        page.locator("[data-action=remove]").click()
        assertEquals("7", page.locator("[data-status]").getAttribute("data-rows"), "back to seven")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the pagination buttons walk the pages`(page: Page) {
        openHome(page)
        goToGrid(page)

        page.locator("[data-action=next]").click()
        assertEquals("2", page.locator("[data-status]").getAttribute("data-page"), "second page")

        page.locator("[data-action=previous]").click()
        assertEquals("1", page.locator("[data-status]").getAttribute("data-page"), "first page again")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the language switch relabels the toolbar, the headers and the pager`(page: Page) {
        openHome(page)
        goToGrid(page)

        page.locator("[data-menu='lang']").first().click()
        page.locator("[data-action='lang:zh-Hans']").click()

        assertEquals("zh-Hans", page.locator("html").getAttribute("lang"))
        assertEquals("表格", page.locator("vaadin-side-nav-item[data-route='grid']").textContent())
        assertEquals("新增一行", page.locator("[data-action=add]").textContent())
        // The grid repaints a frame after the option is set, so the reads retry instead of racing.
        assertThat(page.locator(".ag-header-cell-text").first()).hasText("厂商")
        // The pager comes from the upstream locale table, not from the page's own strings.
        assertThat(page.locator(".ag-paging-description")).containsText("页")
    }

    @RetryingTest(maxAttempts = 2)
    fun `page logs no console errors or failed requests`(page: Page) {
        val consoleErrors = mutableListOf<String>()
        val failedRequests = mutableListOf<String>()
        page.onConsoleMessage { message -> if (message.type() == "error") consoleErrors += message.text() }
        page.onRequestFailed { request -> failedRequests += "${request.url()} ${request.failure()}" }

        openHome(page)
        goToGrid(page)
        page.locator("[data-action=add]").click()

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun goToGrid(page: Page) {
        page.locator("vaadin-side-nav-item[data-route='grid']").click()
        page.locator("[data-status][data-rows='7']").waitFor(Locator.WaitForOptions().setTimeout(20_000.0))
        page.locator(".ag-row").first().waitFor(Locator.WaitForOptions().setTimeout(20_000.0))
    }

    private fun openHome(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
    }
}
