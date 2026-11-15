package demo

import io.github.shijianjs.ktjs.aggrid.probe.ProbeNamespace
import io.github.shijianjs.ktjs.aggrid.probe.createGrid
import io.github.shijianjs.ktjs.aggrid.probe.probeConst
import kotlin.js.console

fun main() {
    // Temporary probe: forces the module's external bindings into the emitted bundle.
    console.log(probeConst)
    console.log(ProbeNamespace.moduleConst, ProbeNamespace.moduleRegistry)
    console.log(createGrid(probeConst))
}
