package com.example

import com.example.data.FilenameValidator
import com.example.data.FormatUtils
import com.example.data.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun urlValidator_acceptsLocalTermuxHttpUrlAndRejectsUnsafeSchemes() {
        val valid = UrlValidator.normalizeAndValidate("http://127.0.0.1:3000/")
        assertTrue(valid.isSuccess)
        assertEquals("http://127.0.0.1:3000", valid.getOrNull())

        assertTrue(UrlValidator.normalizeAndValidate("javascript:alert(1)").isFailure)
        assertTrue(UrlValidator.normalizeAndValidate("file:///etc/passwd").isFailure)
        assertTrue(UrlValidator.normalizeAndValidate("http://127.0.0.1:3000?token=abc").isFailure)
    }

    @Test
    fun filenameValidator_blocksPathTraversal() {
        assertTrue(FilenameValidator.validate("inception_4k.mp4").isSuccess)
        assertTrue(FilenameValidator.validate("../secret.mp4").isFailure)
        assertTrue(FilenameValidator.validate("sub/movie.mp4").isFailure)
        assertTrue(FilenameValidator.validate("movie.mp4?token=1").isFailure)
    }

    @Test
    fun formatUtils_formatsByteSizesAccurately() {
        assertEquals("512 B", FormatUtils.formatBytes(512L))
        assertEquals("1.00 MB", FormatUtils.formatBytes(1024L * 1024L))
    }
}
