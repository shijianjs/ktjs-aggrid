package demo.page.grid

import io.github.shijianjs.ktjs.aggrid.GridApi
import io.github.shijianjs.ktjs.dom.element.invoke

/** One toolbar button: the command it runs on the grid. */
internal class Action(val id: String, val run: (GridPage, GridApi) -> Unit)
