package demo

import io.github.shijianjs.ktjs.aggrid.AG_GRID_LOCALE_EN_US
import io.github.shijianjs.ktjs.aggrid.AG_GRID_LOCALE_ZH_CN
import io.github.shijianjs.ktjs.aggrid.AllCommunityModule
import io.github.shijianjs.ktjs.aggrid.ColDef
import io.github.shijianjs.ktjs.aggrid.GridApi
import io.github.shijianjs.ktjs.aggrid.GridOptions
import io.github.shijianjs.ktjs.aggrid.LocaleText
import io.github.shijianjs.ktjs.aggrid.Module
import io.github.shijianjs.ktjs.aggrid.RowDataTransaction
import io.github.shijianjs.ktjs.aggrid.ThemeOrString
import io.github.shijianjs.ktjs.aggrid.colorSchemeDark
import io.github.shijianjs.ktjs.aggrid.createGrid
import io.github.shijianjs.ktjs.aggrid.moduleRegistryStatics
import io.github.shijianjs.ktjs.aggrid.themeQuartz
import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import web.cssom.ClassName
import web.dom.ElementId
import web.dom.document
import web.events.EventHandler
import web.html.HTMLAnchorElement
import web.html.HTMLButtonElement
import web.html.HTMLDivElement
import web.html.HTMLElement
import web.html.HTMLHeadingElement
import web.html.HTMLImageElement
import web.html.HTMLParagraphElement
import web.html.HTMLSpanElement

/** A row of the demo grid. Upstream row data is untyped, so this is only a Kotlin-side shape. */
external interface Car {
    var make: String
    var model: String
    var price: Double
    var rating: Double
}

/** The demo is bilingual; every string the page prints comes from one of these tables. */
private class Copy(
    val heading: String,
    val tagline: String,
    val hint: String,
    val addRow: String,
    val removeRow: String,
    val light: String,
    val dark: String,
    val docs: String,
    val api: String,
    val source: String,
    val columnMake: String,
    val columnModel: String,
    val columnPrice: String,
    val columnRating: String,
    val rows: (Int) -> String,
    val locale: LocaleText,
    val lang: String,
)

private val english = Copy(
    heading = "ktjs-aggrid demo",
    tagline = "AG Grid Community driven through generated Kotlin/JS bindings.",
    hint = "Sort, filter and page the grid; every option below is type-checked at compile time.",
    addRow = "Add row",
    removeRow = "Remove row",
    light = "Light theme",
    dark = "Dark theme",
    docs = "Documentation",
    api = "API reference",
    source = "Source",
    columnMake = "Make",
    columnModel = "Model",
    columnPrice = "Price",
    columnRating = "Rating",
    rows = { count -> if (count == 1) "1 row" else "$count rows" },
    locale = AG_GRID_LOCALE_EN_US.unsafeCast<LocaleText>(),
    lang = "en",
)

private val chinese = Copy(
    heading = "ktjs-aggrid 示例",
    tagline = "用生成的 Kotlin/JS 绑定驱动 AG Grid 社区版。",
    hint = "排序、筛选、分页都走类型安全的绑定，参数写错在编译期就会报错。",
    addRow = "新增一行",
    removeRow = "删除一行",
    light = "浅色主题",
    dark = "深色主题",
    docs = "文档",
    api = "API 文档",
    source = "源码",
    columnMake = "厂商",
    columnModel = "车型",
    columnPrice = "价格",
    columnRating = "评分",
    rows = { count -> "$count 行" },
    locale = AG_GRID_LOCALE_ZH_CN.unsafeCast<LocaleText>(),
    lang = "zh-Hans",
)

private fun car(make: String, model: String, price: Double, rating: Double): Car = unsafeJso<Car> {
    this.make = make
    this.model = model
    this.price = price
    this.rating = rating
}

private fun initialRows(): MutableList<Car> = mutableListOf(
    car("Toyota", "Corolla", 32000.0, 4.3),
    car("Ford", "Mondeo", 35000.0, 4.1),
    car("Porsche", "Boxster", 72000.0, 4.7),
    car("Honda", "Civic", 29000.0, 4.4),
    car("BMW", "M3", 68000.0, 4.6),
    car("Volkswagen", "Golf", 27000.0, 4.2),
    car("Mazda", "MX-5", 31000.0, 4.5),
)

private fun <T> typed(name: String): T = document.createElement(name).unsafeCast<T>()

/** The page owns the grid: switching language rebuilds it so the column headers relocalise. */
private class DemoPage {
    private val page = typed<HTMLDivElement>("div").apply { className = ClassName("page") }
    private val heading = typed<HTMLHeadingElement>("h1")
    private val tagline = typed<HTMLParagraphElement>("p").apply { className = ClassName("tagline") }
    private val hint = typed<HTMLParagraphElement>("p").apply { className = ClassName("hint") }
    private val gridHost = typed<HTMLDivElement>("div").apply {
        id = ElementId("grid")
        className = ClassName("grid")
    }
    private val status = typed<HTMLSpanElement>("span").apply { className = ClassName("status") }
    private val addButton = typed<HTMLButtonElement>("button").apply { id = ElementId("add-row") }
    private val removeButton = typed<HTMLButtonElement>("button").apply { id = ElementId("remove-row") }
    private val englishButton = typed<HTMLButtonElement>("button").apply {
        id = ElementId("lang-en")
        className = ClassName("chip")
        textContent = "English"
    }
    private val chineseButton = typed<HTMLButtonElement>("button").apply {
        id = ElementId("lang-zh")
        className = ClassName("chip")
        textContent = "中文"
    }
    private val themeButton = typed<HTMLButtonElement>("button").apply {
        id = ElementId("theme")
        className = ClassName("chip")
    }
    private val docsLink = typed<HTMLAnchorElement>("a")
    private val apiLink = typed<HTMLAnchorElement>("a")
    private val sourceLink = typed<HTMLAnchorElement>("a")

    private var copy = english
    private var dark = false
    private var api: GridApi? = null
    private val rows = initialRows()
    private var added = 0

    fun mount() {
        moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))

        page.appendChild(header())
        page.appendChild(toolbar())
        page.appendChild(hint)
        page.appendChild(gridHost)
        page.appendChild(actions())
        page.appendChild(footer())
        document.body.appendChild(page)

        englishButton.onclick = EventHandler { switchTo(english) }
        chineseButton.onclick = EventHandler { switchTo(chinese) }
        themeButton.onclick = EventHandler { toggleTheme() }
        addButton.onclick = EventHandler { addRow() }
        removeButton.onclick = EventHandler { removeRow() }

        switchTo(english)
    }

    private fun header(): HTMLElement {
        val header = typed<HTMLDivElement>("div").apply { className = ClassName("header") }
        val logo = typed<HTMLImageElement>("img").apply {
            className = ClassName("logo")
            src = "img/logo.svg"
            alt = "ktjs-aggrid"
        }
        val titles = typed<HTMLDivElement>("div")
        titles.appendChild(heading)
        titles.appendChild(tagline)
        header.appendChild(logo)
        header.appendChild(titles)
        return header
    }

    private fun toolbar(): HTMLElement {
        val toolbar = typed<HTMLDivElement>("div").apply { className = ClassName("toolbar") }
        toolbar.appendChild(englishButton)
        toolbar.appendChild(chineseButton)
        toolbar.appendChild(themeButton)
        return toolbar
    }

    private fun actions(): HTMLElement {
        val actions = typed<HTMLDivElement>("div").apply { className = ClassName("actions") }
        actions.appendChild(addButton)
        actions.appendChild(removeButton)
        actions.appendChild(status)
        return actions
    }

    private fun footer(): HTMLElement {
        val footer = typed<HTMLDivElement>("div").apply { className = ClassName("footer") }
        footer.appendChild(docsLink)
        footer.appendChild(apiLink)
        footer.appendChild(sourceLink)
        return footer
    }

    private fun switchTo(next: Copy) {
        copy = next
        document.documentElement.lang = next.lang
        document.title = next.heading
        englishButton.disabled = next == english
        chineseButton.disabled = next == chinese
        heading.textContent = next.heading
        tagline.textContent = next.tagline
        hint.textContent = next.hint
        addButton.textContent = next.addRow
        removeButton.textContent = next.removeRow
        docsLink.textContent = next.docs
        apiLink.textContent = next.api
        sourceLink.textContent = next.source
        docsLink.href = "../"
        apiLink.href = "../api/"
        sourceLink.href = "https://github.com/shijianjs/ktjs-aggrid"
        themeButton.textContent = if (dark) next.light else next.dark
        rebuild()
    }

    private fun toggleTheme() {
        dark = !dark
        themeButton.textContent = if (dark) copy.light else copy.dark
        // `theme` is a grid option, so the grid is rebuilt with the recoloured quartz theme.
        rebuild()
    }

    private fun theme() = ThemeOrString(
        if (dark) themeQuartz.withPart(colorSchemeDark) else themeQuartz,
    )

    private fun rebuild() {
        api?.destroy()
        gridHost.replaceChildren()
        api = createGrid(gridHost, options(), null)
        renderStatus()
    }

    private fun column(field: String, header: String): ColDef = unsafeJso<ColDef> {
        this.field = field
        headerName = header
        sortable = true
        filter = true
        resizable = true
    }

    private fun options(): GridOptions = unsafeJso<GridOptions> {
        columnDefs = arrayOf<JsAny>(
            column("make", copy.columnMake),
            column("model", copy.columnModel),
            column("price", copy.columnPrice),
            column("rating", copy.columnRating),
        )
        rowData = Array<JsAny>(rows.size) { rows[it] }
        defaultColDef = unsafeJso<ColDef> {
            flex = 1.0
        }
        pagination = true
        paginationPageSize = 5.0
        localeText = copy.locale
        theme = theme()
        animateRows = true
    }

    private fun addRow() {
        added += 1
        val row = car("Nissan", "Note #$added", 25000.0 + added * 100.0, 4.0)
        rows.add(row)
        api?.applyTransaction(unsafeJso<RowDataTransaction> {
            add = arrayOf<JsAny>(row)
        })
        renderStatus()
    }

    private fun removeRow() {
        if (rows.size <= 1) {
            return
        }
        val row = rows.removeAt(rows.lastIndex)
        api?.applyTransaction(unsafeJso<RowDataTransaction> {
            remove = arrayOf<JsAny>(row)
        })
        renderStatus()
    }

    private fun renderStatus() {
        val displayed = api?.getDisplayedRowCount()?.toInt() ?: rows.size
        status.textContent = copy.rows(displayed)
    }
}

fun main() {
    DemoPage().mount()
}
