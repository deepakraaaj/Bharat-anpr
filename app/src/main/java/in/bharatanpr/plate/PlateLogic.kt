package com.bharatanpr.plate

data class ParsedPlate(val normalized: String, val format: PlateFormat, val validationConfidence: Float, val segments: List<String>)
enum class PlateFormat { STANDARD, BHARAT_SERIES, UNKNOWN }
object PlateNormalizer { fun normalize(value:String)=value.uppercase().filter { it in 'A'..'Z'||it in '0'..'9' } }
object IndianStateCodes {
    val all=setOf("AN","AP","AR","AS","BR","CG","CH","DD","DL","DN","GA","GJ","HP","HR","JH","JK","KA","KL","LA","LD","MH","ML","MN","MP","MZ","NL","OD","OR","PB","PY","RJ","SK","TN","TR","TS","UK","UP","WB")
}
object OcrErrorCorrector {
    private val toDigit=mapOf('O' to '0','Q' to '0','U' to '0','D' to '0','I' to '1','L' to '4','Z' to '2','S' to '5','G' to '6','B' to '8')
    private val toLetter=mapOf('0' to 'O','1' to 'I','2' to 'Z','5' to 'S','6' to 'G','8' to 'B')
    fun correct(input:String):String {
        val s=PlateNormalizer.normalize(input)
        if(s.length==10 && s.substring(2,4).equals("BH",true)) return mapPattern(s,"DDAADDDDAA")
        if(s.length !in 9..11) return s
        val state=s.take(2).map{toLetter[it]?:it}.joinToString("")
        if(state !in IndianStateCodes.all) return s
        val district=s.substring(2,4).map{toDigit[it]?:it}.joinToString("")
        val last=s.takeLast(4).map{toDigit[it]?:it}.joinToString("")
        val series=s.substring(4,s.length-4).map{toLetter[it]?:it}.joinToString("")
        return state+district+series+last
    }
    private fun mapPattern(s:String,p:String)=s.mapIndexed{i,c->if(p[i]=='D')toDigit[c]?:c else toLetter[c]?:c}.joinToString("")
}
object IndianPlateParser {
    private val standard=Regex("^([A-Z]{2})([0-9]{1,2})([A-Z]{1,3})([0-9]{1,4})$")
    private val bharat=Regex("^([0-9]{2})(BH)([0-9]{4})([A-Z]{2})$")
    fun parse(raw:String):ParsedPlate {
        val normalized=PlateNormalizer.normalize(raw)
        val candidates=buildList {
            add(normalized)
            for(length in 11 downTo 9) if(normalized.length>=length) for(start in 0..normalized.length-length) add(normalized.substring(start,start+length))
        }.distinct()
        candidates.forEach { candidate ->
            val corrected=OcrErrorCorrector.correct(candidate)
            bharat.matchEntire(corrected)?.let{return ParsedPlate(corrected,PlateFormat.BHARAT_SERIES,.98f,it.groupValues.drop(1))}
            standard.matchEntire(corrected)?.let { m-> if(m.groupValues[1] in IndianStateCodes.all)return ParsedPlate(corrected,PlateFormat.STANDARD,.97f,m.groupValues.drop(1)) }
        }
        val corrected=OcrErrorCorrector.correct(normalized)
        standard.matchEntire(corrected)?.let { m->return ParsedPlate(corrected,PlateFormat.STANDARD,.48f,m.groupValues.drop(1)) }
        return ParsedPlate(corrected,PlateFormat.UNKNOWN,0f,emptyList())
    }
    fun isCompleteScanResult(plate: ParsedPlate): Boolean = when (plate.format) {
        PlateFormat.STANDARD -> plate.segments.lastOrNull()?.length == 4
        PlateFormat.BHARAT_SERIES -> plate.segments.getOrNull(2)?.length == 4 && plate.segments.lastOrNull()?.length == 2
        PlateFormat.UNKNOWN -> false
    }
}
object IndianPlateValidator { fun score(value:String)=IndianPlateParser.parse(value).validationConfidence; fun isValid(value:String)=score(value)>=.75f }
