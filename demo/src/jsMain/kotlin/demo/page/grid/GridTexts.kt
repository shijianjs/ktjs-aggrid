package demo.page.grid

import io.github.shijianjs.ktjs.aggrid.LocaleText
import io.github.shijianjs.ktjs.dom.element.invoke

/** Every string this page shows, in one language. */
internal class GridTexts(
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
