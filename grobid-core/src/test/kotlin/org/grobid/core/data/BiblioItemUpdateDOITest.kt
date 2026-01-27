package org.grobid.core.data

import org.grobid.core.main.LibraryLoader
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BiblioItemUpdateDOITest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        LibraryLoader.load()
    }

    @Test
    fun testUpdateDOI_newExtends_shouldReplace() {
        // Case: truncated DOI gets completed (new DOI starts with old and is longer)
        val dest = BiblioItem()
        dest.setDOI("10.1007/s13280-020-01405")
        
        dest.updateDOIIfLonger("10.1007/s13280-020-01405-w")
        
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1007/s13280-020-01405-w"))
    }

    @Test
    fun testUpdateDOI_garbageAppended_shouldNotReplace() {
        // Case: DOI with page numbers concatenated - should be rejected
        // "10.1073/pnas.22211031201of12" does NOT start with "10.1073/pnas.2221103120"
        val dest = BiblioItem()
        dest.setDOI("10.1073/pnas.2221103120")
        
        dest.updateDOIIfLonger("10.1073/pnas.22211031201of12")
        
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1073/pnas.2221103120"))
    }

    @Test
    fun testUpdateDOI_headerEmpty_shouldReplace() {
        val dest = BiblioItem()
        dest.setDOI(null)
        val found = "doi:10.1109/5.771073"

        dest.updateDOIIfLonger(found)

        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1109/5.771073"))
    }

    @Test
    fun testUpdateDOI_headerLonger_shouldNotReplace() {
        // New DOI is shorter - should not replace
        val dest = BiblioItem()
        dest.setDOI("10.1000/valid.long.doi.suffix")
        val found = "10.1000/valid.lo" // truncated

        dest.updateDOIIfLonger(found)

        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1000/valid.long.doi.suffix"))
    }

    @Test
    fun testUpdateDOI_differentDOI_shouldNotReplace() {
        // New DOI doesn't start with old - should not replace
        val dest = BiblioItem()
        dest.setDOI("10.1000/short")
        val found = "10.1000/longer.suffix/with/more"

        dest.updateDOIIfLonger(found)

        // Should NOT replace because new doesn't start with old
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1000/short"))
    }

    @Test
    fun testUpdateDOI_sameDOI_shouldNotChange() {
        val dest = BiblioItem()
        dest.setDOI("10.1007/s13280-020-01405-w")
        
        dest.updateDOIIfLonger("10.1007/s13280-020-01405-w")
        
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1007/s13280-020-01405-w"))
    }

    @Test
    fun testUpdateDOI_cleansPrefixBeforeComparison() {
        val dest = BiblioItem()
        dest.setDOI("10.1007/s13280-020-01405")
        
        // New DOI has https prefix that should be cleaned
        dest.updateDOIIfLonger("https://doi.org/10.1007/s13280-020-01405-w")
        
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1007/s13280-020-01405-w"))
    }

    @Test
    fun testUpdateDOI_nullNew_shouldNotChange() {
        val dest = BiblioItem()
        dest.setDOI("10.1007/s13280-020-01405-w")
        
        dest.updateDOIIfLonger(null)
        
        Assert.assertThat(dest.getDOI(), Matchers.`is`("10.1007/s13280-020-01405-w"))
    }
}
