package com.bharatanpr.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundingBoxTest{@Test fun iouIsCorrect(){assertEquals(1f,BoundingBox(0f,0f,10f,10f).iou(BoundingBox(0f,0f,10f,10f)),.001f);assertEquals(0f,BoundingBox(0f,0f,2f,2f).iou(BoundingBox(3f,3f,5f,5f)),.001f);assertEquals(1f/7f,BoundingBox(0f,0f,2f,2f).iou(BoundingBox(1f,1f,3f,3f)),.001f)}}
