package com.bharatanpr.plate

import org.junit.Assert.*
import org.junit.Test

class PlateLogicTest {
    @Test fun normalizesWhitespaceAndCase(){assertEquals("TN01AB1234",PlateNormalizer.normalize("tn 01-ab 1234"))}
    @Test fun validatesCommonStates(){listOf("TN01AB1234","KA03MN1234","MH12DE1433","DL01ABC1234").forEach{assertTrue(it,IndianPlateValidator.isValid(it))}}
    @Test fun validatesBharatSeries(){val p=IndianPlateParser.parse("22BH1234AA");assertEquals(PlateFormat.BHARAT_SERIES,p.format);assertTrue(p.validationConfidence>.9f)}
    @Test fun correctsBySegmentOnly(){assertEquals("TN01AB1234",OcrErrorCorrector.correct("TN O1 AB I234"));assertEquals("TN01BO1234",OcrErrorCorrector.correct("TN01BO1234"))}
    @Test fun rejectsNoise(){assertFalse(IndianPlateValidator.isValid("HELLOWORLD"));assertFalse(IndianPlateValidator.isValid("123456"))}
    @Test fun recoversPlateFromEdgeNoise(){assertEquals("JH03MF4477",IndianPlateParser.parse("0JHO3MF4477").normalized);assertEquals("JH03MF4477",IndianPlateParser.parse("JHO3MF4477Z").normalized)}
    @Test fun correctsOpenTopFourOnCommercialPlate(){assertEquals("RJ45CM2308",IndianPlateParser.parse("RJL5CM2308").normalized)}
}
