package com.bharatanpr.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundingBoxTest{@Test fun iouIsCorrect(){assertEquals(1f,BoundingBox(0f,0f,10f,10f).iou(BoundingBox(0f,0f,10f,10f)),.001f);assertEquals(0f,BoundingBox(0f,0f,2f,2f).iou(BoundingBox(3f,3f,5f,5f)),.001f);assertEquals(1f/7f,BoundingBox(0f,0f,2f,2f).iou(BoundingBox(1f,1f,3f,3f)),.001f)}}

class DetectionRegionTest {
    @Test fun centeredRegionMapsToGuideInFillCenterPreview() {
        val previewWidth = 360
        val previewHeight = 640
        val region = DetectionRegion.centeredInPreview(1280, 720, previewWidth, previewHeight)
        val mapped = DetectionRegion.frameToPreview(region, 1280, 720, previewWidth.toFloat(), previewHeight.toFloat())

        assertEquals(previewWidth * DetectionRegion.LEFT_FRACTION, mapped.left, 0.01f)
        assertEquals(previewHeight * DetectionRegion.TOP_FRACTION, mapped.top, 0.01f)
        assertEquals(previewWidth * DetectionRegion.RIGHT_FRACTION, mapped.right, 0.01f)
        assertEquals(previewHeight * DetectionRegion.BOTTOM_FRACTION, mapped.bottom, 0.01f)
    }
}
