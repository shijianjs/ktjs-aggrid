package demo.e2e

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitUntilState
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junitpioneer.jupiter.RetryingTest

/**
 * End-to-end checks for the demo page: the Kotlin/JS bundle has to reach the real AG Grid build,
 * render a grid, and switch both languages and themes without console errors.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest {
    @RetryingTest(maxAttempts = 2)
    fun `page renders the heading, the logo and its stylesheet`(page: Page) {
        openPage(page)

        assertTrue(page.title().contains("ktjs-aggrid"), "title: ${page.title()}")
        assertEquals("ktjs-aggrid demo", page.locator("h1").textContent())

        // The logo is copied in from the site's static files by the `brandAssets` task.
        assertTrue(page.locator("img.logo").isVisible(), "the demo serves the project logo")

        val display = page.locator(".page").evaluate("(node) => getComputedStyle(node).display") as String
        assertTrue(display != "contents", "the stylesheet applies to .page (display = $display)")
    }

    @RetryingTest(maxAttempts = 2)
    fun `the wrapped grid renders rows and columns`(page: Page) {
        openPage(page)

        // Five rows per page, as configured through `paginationPageSize`.
        assertThat(page.locator(".ag-row")).hasCount(5)
        assertThat(page.locator(".ag-header-cell-text").first()).hasText("Make")
        assertEquals("7 rows", page.locator(".status").textContent())

        val theme = page.locator("#grid").getAttribute("class")
        assertTrue(theme?.contains("grid") == true, "grid host class: $theme")
    }

    @RetryingTest(maxAttempts = 2)
    fun `switching the language relocalises the page, the headers and the grid`(page: Page) {
        openPage(page)

        page.locator("#lang-zh").click()

        assertEquals("ktjs-aggrid 示例", page.locator("h1").textContent())
        assertEquals("zh-Hans", page.locator("html").getAttribute("lang"))
        assertThat(page.locator(".ag-header-cell-text").first()).hasText("厂商")
        assertEquals("7 行", page.locator(".status").textContent())
        // The paging panel comes from the upstream locale table, not from the demo's own strings.
        assertThat(page.locator(".ag-paging-description")).containsText("页")

        page.locator("#lang-en").click()

        assertEquals("ktjs-aggrid demo", page.locator("h1").textContent())
        assertThat(page.locator(".ag-header-cell-text").first()).hasText("Make")
    }

    @RetryingTest(maxAttempts = 2)
    fun `adding and removing rows goes through the grid API`(page: Page) {
        openPage(page)

        page.locator("#add-row").click()
        assertEquals("8 rows", page.locator(".status").textContent())

        page.locator("#remove-row").click()
        assertEquals("7 rows", page.locator(".status").textContent())
    }

    @RetryingTest(maxAttempts = 2)
    fun `the theme toggle repaints the grid`(page: Page) {
        openPage(page)

        val light = page.locator(".ag-root-wrapper").evaluate("(node) => getComputedStyle(node).backgroundColor")
        page.locator("#theme").click()
        // The rebuild swaps the quartz theme for quartz + the dark colour scheme part.
        page.waitForSelector(".ag-row")
        val dark = page.locator(".ag-root-wrapper").evaluate("(node) => getComputedStyle(node).backgroundColor")

        assertTrue(light != dark, "grid background did not change: $light / $dark")
    }

    @RetryingTest(maxAttempts = 2)
    fun `page logs no console errors or failed requests`(page: Page) {
        val consoleErrors = mutableListOf<String>()
        val failedRequests = mutableListOf<String>()
        page.onConsoleMessage { message ->
            if (message.type() == "error") consoleErrors += message.text()
        }
        page.onRequestFailed { request ->
            failedRequests += "${request.url()} ${request.failure()}"
        }

        openPage(page)

        assertTrue(consoleErrors.isEmpty(), "console errors: $consoleErrors")
        assertTrue(failedRequests.isEmpty(), "failed requests: $failedRequests")
    }

    private fun openPage(page: Page) {
        page.navigate("/", Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE))
        page.waitForSelector(".ag-row")
    }
}
