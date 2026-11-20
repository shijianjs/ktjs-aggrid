package demo.page.grid

import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
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
import io.github.shijianjs.ktjs.aggrid.createGrid
import io.github.shijianjs.ktjs.aggrid.moduleRegistryStatics
import io.github.shijianjs.ktjs.aggrid.themeQuartz
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.page.newReadout
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.grid
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.dom.element.inlineStyle
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.vaadin.button.ButtonElement
import io.github.shijianjs.ktjs.vaadin.button.createButton
import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import web.animations.requestAnimationFrame
import web.cssom.px
import web.dom.clickEvent
import web.events.addHandler
import web.html.HTMLElement
import web.html.HtmlTagName

/** A row of the demo grid. Upstream row data is untyped, so this is only a Kotlin-side shape. */
private external interface Car {
    var make: String
    var model: String
    var price: Double
    var rating: Double
}

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

/** Every string this page shows, in one language. */
private class GridTexts(
    val title: String,
    val summary: String,
    val gridHeading: String,
    val hint: String,
    /** The toolbar labels, in the order of [ACTIONS]. */
    val labels: List<String>,
    val columnMake: String,
    val columnModel: String,
    val columnPrice: String,
    val columnRating: String,
    val rows: String,
    val page: String,
    val locale: LocaleText,
)

private val gridEnglish: GridTexts = GridTexts(
    title = "Grid",
    summary = "The real AG Grid Community engine behind the wrapper: sort a column by its header, filter it " +
        "from the menu, and page through the rows while the toolbar drives the same grid through " +
        "`applyTransaction` and `paginationGoToNextPage`.",
    gridHeading = "Live grid",
    hint = "Everything below is the wrapper's API: `createGrid`, `applyTransaction`, `setGridOption` and the " +
        "pagination calls.",
    labels = listOf("Add row", "Remove row", "Previous page", "Next page"),
    columnMake = "Make",
    columnModel = "Model",
    columnPrice = "Price",
    columnRating = "Rating",
    rows = "Rows",
    page = "Page",
    locale = AG_GRID_LOCALE_EN_US.unsafeCast<LocaleText>(),
)

private val gridChinese: GridTexts = GridTexts(
    title = "表格",
    summary = "封装背后的真实 AG Grid 社区版引擎：点表头排序、从菜单里筛选、翻页，工具栏则通过 " +
        "`applyTransaction` 与 `paginationGoToNextPage` 操作同一张表。",
    gridHeading = "实时表格",
    hint = "下面的一切都来自 wrapper 的 API：`createGrid`、`applyTransaction`、`setGridOption` 以及分页那几个调用。",
    labels = listOf("新增一行", "删除一行", "上一页", "下一页"),
    columnMake = "厂商",
    columnModel = "车型",
    columnPrice = "价格",
    columnRating = "评分",
    rows = "行数",
    page = "页码",
    locale = AG_GRID_LOCALE_ZH_CN.unsafeCast<LocaleText>(),
)

private val gridTexts: Translations<GridTexts> = texts(
    Langs.EN to gridEnglish,
    Langs.ZH_HANS to gridChinese,
)

/** One toolbar button: the command it runs on the grid. */
private class Action(val id: String, val run: (GridPage, GridApi) -> Unit)

private val ACTIONS: List<Action> = listOf(
    Action("add") { page, api -> page.addRow(api) },
    Action("remove") { page, api -> page.removeRow(api) },
    Action("previous") { page, api -> api.paginationGoToPreviousPage(); page.refresh(api) },
    Action("next") { page, api -> api.paginationGoToNextPage(); page.refresh(api) },
)

/**
 * The grid page: a toolbar, the live grid, and a readout of how many rows it holds and which page it
 * shows.
 *
 * AG Grid paints itself: its theming is a JavaScript value (`themeQuartz`), not a stylesheet, so the
 * page re-hosts nothing. The host only needs a definite height, because the grid sizes its viewport
 * from the element it was created on.
 */
internal class GridPage : DemoPage() {

    private val texts = Binder(gridTexts)

    /** The strings the column headers and the readout use; kept in step with the switch. */
    private var current: GridTexts = gridEnglish

    private var api: GridApi? = null

    private val rows = initialRows()

    /** Counts the rows the toolbar added, so a click is visible even when the grid looks alike. */
    private var added = 0

    private val host: HTMLElement = HtmlTagName.div {
        setAttribute("data-grid", "")
        inlineStyle.apply {
            height = 520.px
        }
    }

    /** Rows and page: one line under the toolbar, marked for the e2e suite. */
    private val status = newReadout()

    /** One button per action, each tagged so the end-to-end suite can address it by name. */
    private val buttons: List<ButtonElement> = ACTIONS.map { action ->
        createButton { setAttribute("data-action", action.id) }
    }


    init {
        // AG Grid 33+ is modular: an unregistered capability is simply absent, and nothing complains.
        moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))

        val section = sectionBlock()
        texts.bind(section.heading, { it.gridHeading })
        texts.bind(section.hint, { it.hint })
        section.body.append(
            grid(minColumnWidth = 150, *buttons.toTypedArray()),
            status.root,
            exampleBox(null, host),
        )

        body.append(section.root)
        status.root.setAttribute("data-status", "")

        buttons.forEachIndexed { index, button ->
            val action = ACTIONS[index]
            button.clickEvent.addHandler {
                val target = api ?: return@addHandler
                action.run(this, target)
            }
        }

        createOnceMounted()
    }

    /**
     * Creates the grid as soon as the host is in the document *and* laid out.
     *
     * A grid created while the page is still being attached measures a zero-width viewport and lays
     * its columns out against that; the browser does not redo it afterwards.
     */
    private fun createOnceMounted() {
        if (api != null) return
        if (!host.isConnected || host.clientWidth < 200) {
            requestAnimationFrame { _ -> createOnceMounted() }
            return
        }
        api = createGrid(host, options(), null)
        refresh(api!!)
    }

    private fun column(field: String, header: String): ColDef = unsafeJso<ColDef> {
        this.field = field
        headerName = header
        sortable = true
        filter = true
        resizable = true
    }

    private fun columns(): Array<JsAny> = arrayOf(
        column("make", current.columnMake),
        column("model", current.columnModel),
        column("price", current.columnPrice),
        column("rating", current.columnRating),
    )

    private fun options(): GridOptions = unsafeJso<GridOptions> {
        columnDefs = columns()
        rowData = Array<JsAny>(rows.size) { rows[it] }
        defaultColDef = unsafeJso<ColDef> {
            flex = 1.0
        }
        pagination = true
        paginationPageSize = 5.0
        localeText = current.locale
        theme = ThemeOrString(themeQuartz)
        animateRows = true
    }

    /** Adds one row after the sample set; the model name makes the new row identifiable. */
    fun addRow(api: GridApi) {
        added += 1
        val row = car("Nissan", "Note #$added", 25000.0 + added * 100.0, 4.0)
        rows.add(row)
        api.applyTransaction(unsafeJso<RowDataTransaction> {
            add = arrayOf<JsAny>(row)
        })
        refresh(api)
    }

    /** Removes the last row the page knows about; the sample set itself is left alone. */
    fun removeRow(api: GridApi) {
        if (rows.size <= 1) return
        val row = rows.removeAt(rows.lastIndex)
        api.applyTransaction(unsafeJso<RowDataTransaction> {
            remove = arrayOf<JsAny>(row)
        })
        refresh(api)
    }

    /** Re-reads the grid: how many rows it displays and which page it is on. */
    fun refresh(api: GridApi) {
        val displayed = api.getDisplayedRowCount().toInt()
        val pageNumber = api.paginationGetCurrentPage().toInt() + 1
        val pages = api.paginationGetTotalPages().toInt()
        status.set("${current.rows}: ${rows.size} · ${current.page}: $pageNumber/$pages")
        status.root.setAttribute("data-rows", "${rows.size}")
        status.root.setAttribute("data-displayed", "$displayed")
        status.root.setAttribute("data-page", "$pageNumber")
        status.root.setAttribute("data-pages", "$pages")
    }

    override fun applyTranslation(lang: Lang) {
        current = gridTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)
        buttons.forEachIndexed { index, button ->
            button.textContent = current.labels.getOrElse(index) { "" }
        }
        // AG Grid takes its locale at creation: `setGridOption("localeText", …)` reaches the headers,
        // but the pager is a component that only re-renders when its own options change, and forcing
        // it leaves the page numbers blank. So the grid is recreated — the strings are right in one
        // frame, at the cost of the sort and filter the user had set up. The rows, the page size and
        // the column definitions are the page's own state and survive.
        api?.destroy()
        api = null
        host.replaceChildren()
        createOnceMounted()
    }

    companion object : CustomElementReference<GridPage>(GridPage::class.js) {
        val NAME: TagName<GridPage> = TagName("aggrid-grid-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<aggrid-grid-page>`. */
internal fun newGridPage(): GridPage {
    GridPage.define()
    return GridPage.NAME()
}
