package demo.page.overview

import demo.API_URL
import demo.DOCS_URL
import demo.SOURCE_URL
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.page.anchor
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.newPage
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import web.html.HtmlTagName

/** Every string this page shows, in one language. */
private class OverviewTexts(
    val title: String,
    val summary: String,
    val docsLink: String,
    val sourceLink: String,
    val apiLink: String,
    val purposeHeading: String,
    val body: String,
    val bullets: List<String>,
    val optionsHeading: String,
    val optionsBody: String,
    val nextHeading: String,
    val nextBody: String,
)

private val overviewEnglish: OverviewTexts = OverviewTexts(
    title = "Overview",
    summary = "A Kotlin/JS wrapper for AG Grid Community: typed column definitions, typed grid options, " +
        "and the grid API reachable from Kotlin.",
    docsLink = "Documentation",
    sourceLink = "Source repository",
    apiLink = "API reference",
    purposeHeading = "What it wraps",
    body = "AG Grid renders a data grid: sorting, filtering, pagination and a hundred options. The wrapper " +
        "gives those options Kotlin types, so `colDef { field = \"price\"; filter = true }` and " +
        "`gridOptions { pagination = true; paginationPageSize = 5.0 }` are checked at compile time instead " +
        "of being string-keyed literals, and the runtime API comes back as `GridApi`.",
    bullets = listOf(
        "createGrid(host, gridOptions { … }, null) — the grid, over one element.",
        "api.applyTransaction(unsafeJso<RowDataTransaction> { add = … }) — rows change in place.",
        "api.setGridOption(\"localeText\", …) — an option changed after creation, still typed.",
    ),
    optionsHeading = "Modules are explicit",
    optionsBody = "AG Grid 33+ is modular: a capability that is not registered is not there, and asking for " +
        "it fails silently. The wrapper re-exports the community modules, so " +
        "`moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))` is the one line " +
        "that stands between an empty grid and a working one.",
    nextHeading = "Reading order",
    nextBody = "The overview, then the grid: sort a column, filter it, page through the rows, and watch the " +
        "readout follow the API.",
)

private val overviewChinese: OverviewTexts = OverviewTexts(
    title = "概览",
    summary = "AG Grid 社区版的 Kotlin/JS 封装：列定义有类型、网格选项有类型，网格 API 也能直接从 Kotlin 调用。",
    docsLink = "使用文档",
    sourceLink = "源码仓库",
    apiLink = "API 文档",
    purposeHeading = "它封装了什么",
    body = "AG Grid 渲染一张数据表格：排序、筛选、分页，外加上百个选项。封装把这些选项变成 Kotlin 类型：" +
        "`colDef { field = \"price\"; filter = true }`、`gridOptions { pagination = true; paginationPageSize = 5.0 }` " +
        "在编译期就被检查，而不是字符串键值；运行期 API 以 `GridApi` 的形式返回。",
    bullets = listOf(
        "createGrid(host, gridOptions { … }, null) —— 一个元素之上的一个表格。",
        "api.applyTransaction(unsafeJso<RowDataTransaction> { add = … }) —— 原地增删行。",
        "api.setGridOption(\"localeText\", …) —— 创建之后再改选项，依然有类型。",
    ),
    optionsHeading = "模块要显式注册",
    optionsBody = "AG Grid 33+ 是模块化的：没注册的能力就等于不存在，而缺失时它只会静默地什么都不做。" +
        "封装重新导出了社区版模块，于是 " +
        "`moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))` 就是空表格与可用表格之间" +
        "那唯一的一行。",
    nextHeading = "建议顺序",
    nextBody = "先看概览，再看表格：排一列、筛一列、翻页，读数会跟着 API 走。",
)

private val overviewTexts: Translations<OverviewTexts> = texts(
    Langs.EN to overviewEnglish,
    Langs.ZH_HANS to overviewChinese,
)

/**
 * Landing page: what the library wraps, how it is consumed, where the reference lives.
 */
internal class OverviewPage : Page {
    private val page = newPage()

    private val texts = Binder(overviewTexts)

    override val node get() = page

    init {
        page.addLink(texts.bind(anchor("", DOCS_URL, external = true), { it.docsLink }))
        page.addLink(texts.bind(anchor("", API_URL, external = true), { it.apiLink }))
        page.addLink(texts.bind(anchor("", SOURCE_URL, external = true), { it.sourceLink }))

        val purpose = sectionBlock()
        texts.bind(purpose.title, { it.purposeHeading })
        // One bound <li> per bullet, so a language switch rewrites each line in place.
        val bullets = HtmlTagName.ul {
            List(3) { index -> texts.bind(+HtmlTagName.li()) { it.bullets.getOrNull(index).orEmpty() } }
        }
        purpose.body.append(texts.bind(paragraph(), { it.body }), bullets)

        val options = sectionBlock()
        texts.bind(options.title, { it.optionsHeading })
        options.body.append(
            texts.bind(paragraph(), { it.optionsBody }),
            row(codeText("moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))")),
        )

        val next = sectionBlock()
        texts.bind(next.title, { it.nextHeading })
        next.body.append(texts.bind(paragraph(), { it.nextBody }))

        page.body.append(purpose.root, options.root, next.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = overviewTexts[lang]
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
    }
}
