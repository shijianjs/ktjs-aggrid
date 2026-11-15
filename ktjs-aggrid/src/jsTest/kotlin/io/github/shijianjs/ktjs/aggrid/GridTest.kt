package io.github.shijianjs.ktjs.aggrid

import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.unsafeCast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import web.dom.document
import web.html.HTMLDivElement

/** A row object of the test grid; upstream row data is untyped, so any shape is legal. */
external interface Car {
    var make: String
    var model: String
    var price: Double
}

/**
 * The wrapper only earns its name if the ESM bindings reach the installed AG Grid build, so these
 * tests import the real `ag-grid-community` package and drive a grid inside a real browser.
 */
class GridTest {
    private fun host(): HTMLDivElement {
        val element = document.createElement("div").unsafeCast<HTMLDivElement>()
        element.style.width = "600px"
        element.style.height = "300px"
        document.body.appendChild(element)
        return element
    }

    private fun column(field: String, header: String): JsAny = unsafeJso<ColDef> {
        this.field = field
        headerName = header
        sortable = true
    }.unsafeCast<JsAny>()

    private fun car(make: String, model: String, price: Double): JsAny = unsafeJso<Car> {
        this.make = make
        this.model = model
        this.price = price
    }.unsafeCast<JsAny>()

    @Test
    fun communityModuleRegistersThroughTheStaticBinding() {
        moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))
    }

    @Test
    fun gridIsCreatedThroughTheWrappedFactoryAndHoldsTheRows() {
        moduleRegistryStatics.registerModules(arrayOf<Module>(AllCommunityModule))
        val element = host()
        val options = unsafeJso<GridOptions> {
            columnDefs = arrayOf(column("make", "Make"), column("model", "Model"), column("price", "Price"))
            rowData = arrayOf(car("Toyota", "Corolla", 35000.0), car("Ford", "Mondeo", 32000.0))
        }

        val api = createGrid(element, options, null)
        try {
            assertEquals(2.0, api.getDisplayedRowCount())
            assertEquals(3, api.getColumnDefs()?.size)
            assertNotNull(api.getGridOption("rowData".unsafeCast<JsAny>()))
            assertNotNull(api.getRowNode("0"))
        } finally {
            api.destroy()
            element.remove()
        }
    }

    @Test
    fun upstreamLiteralsKeepTheirWireValues() {
        assertEquals("left", LabelAlignments.left.unsafeCast<String>())
        assertEquals("right", LabelAlignments.right.unsafeCast<String>())
        assertEquals("top", LabelAlignments.top.unsafeCast<String>())
    }

    @Test
    fun localePackageExportsTranslatedStrings() {
        assertTrue(AG_GRID_LOCALE_EN_US.selectAll.isNotEmpty())
        assertTrue(AG_GRID_LOCALE_ZH_CN.selectAll.isNotEmpty())
        assertTrue(AG_GRID_LOCALE_ZH_CN.selectAll != AG_GRID_LOCALE_EN_US.selectAll)
    }

    @Test
    fun columnDefinitionWritesTheSameJavaScriptPropertyTheWrapperReads() {
        val colDef = unsafeJso<ColDef> {
            field = "make"
            headerName = "Make"
        }
        assertEquals("make", colDef.field)
        assertEquals("Make", colDef.headerName)
    }
}
