package com.example.domain

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RegionFetchResult<T>(
    val items: List<T>,
    val isFromOfflineFallback: Boolean,
)

object IndonesiaRegionService {

    private val provincesCache = mutableListOf<IndonesiaProvince>()
    private val regenciesCache = ConcurrentHashMap<String, List<IndonesiaRegency>>()

    suspend fun getProvinces(forceRefresh: Boolean = false): RegionFetchResult<IndonesiaProvince> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && provincesCache.isNotEmpty()) {
                return@withContext RegionFetchResult(provincesCache.toList(), isFromOfflineFallback = false)
            }

            val fromV2 = runCatching {
                fetchUrl("https://www.emsifa.com/api-wilayah-indonesia/v2/provinces.json")
            }.getOrNull()?.let { parseProvincesJson(it) }.orEmpty()

            val resolved = if (fromV2.isNotEmpty()) {
                fromV2
            } else {
                runCatching {
                    fetchUrl("https://www.emsifa.com/api-wilayah-indonesia/api/provinces.json")
                }.getOrNull()?.let { parseProvincesJson(it) }.orEmpty()
            }

            if (resolved.isNotEmpty()) {
                synchronized(provincesCache) {
                    provincesCache.clear()
                    provincesCache.addAll(resolved)
                }
                RegionFetchResult(resolved, isFromOfflineFallback = false)
            } else {
                RegionFetchResult(embeddedFallbackProvinces(), isFromOfflineFallback = true)
            }
        }

    suspend fun getRegencies(
        province: IndonesiaProvince,
        forceRefresh: Boolean = false,
    ): RegionFetchResult<IndonesiaRegency> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            regenciesCache[province.id]?.let { cached ->
                return@withContext RegionFetchResult(cached, isFromOfflineFallback = false)
            }
        }

        val cleanProvinceId = province.id.trim()
        val fromV2 = runCatching {
            fetchUrl("https://www.emsifa.com/api-wilayah-indonesia/v2/regencies/$cleanProvinceId.json")
        }.getOrNull()?.let { parseRegenciesJson(it, cleanProvinceId, province.name) }.orEmpty()

        val resolved = if (fromV2.isNotEmpty()) {
            fromV2
        } else {
            runCatching {
                fetchUrl("https://www.emsifa.com/api-wilayah-indonesia/api/regencies/$cleanProvinceId.json")
            }.getOrNull()?.let { parseRegenciesJson(it, cleanProvinceId, province.name) }.orEmpty()
        }

        if (resolved.isNotEmpty()) {
            regenciesCache[cleanProvinceId] = resolved
            RegionFetchResult(resolved, isFromOfflineFallback = false)
        } else {
            RegionFetchResult(
                embeddedFallbackRegencies(cleanProvinceId, province.name),
                isFromOfflineFallback = true,
            )
        }
    }

    internal fun parseProvincesJson(rawJson: String): List<IndonesiaProvince> {
        val trimmed = rawJson.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }
        val list = mutableListOf<IndonesiaProvince>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val rawName = obj.optString("name").trim()
            if (id.isNotEmpty() && rawName.isNotEmpty()) {
                list.add(
                    IndonesiaProvince(
                        id = id,
                        name = formatRegionTitle(rawName),
                    ),
                )
            }
        }
        return list.sortedBy { it.name }
    }

    internal fun parseRegenciesJson(
        rawJson: String,
        provinceId: String,
        provinceName: String = "",
    ): List<IndonesiaRegency> {
        val trimmed = rawJson.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
            else -> JSONArray()
        }
        val list = mutableListOf<IndonesiaRegency>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim()
            val rawName = obj.optString("name").trim()
            if (id.isEmpty() || rawName.isEmpty()) continue
            val lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null
            val lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null
            list.add(
                IndonesiaRegency(
                    id = id,
                    provinceId = obj.optString("province_id").ifBlank { provinceId },
                    name = formatRegionTitle(rawName),
                    provinceName = provinceName,
                    latitude = lat?.takeIf { !it.isNaN() },
                    longitude = lng?.takeIf { !it.isNaN() },
                ),
            )
        }
        return list.sortedBy { it.name }
    }

    internal fun formatRegionTitle(raw: String): String {
        val acronyms = setOf("DKI", "DI", "DIY", "RI", "NTB", "NTT")
        return raw.trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                val upper = word.uppercase(Locale.ROOT)
                when {
                    upper in acronyms -> upper
                    upper == "KAB." -> "Kab."
                    else -> upper.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                    }
                }
            }
    }

    private fun fetchUrl(endpoint: String): String? {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun embeddedFallbackProvinces(): List<IndonesiaProvince> = listOf(
        IndonesiaProvince("11", "Aceh"),
        IndonesiaProvince("12", "Sumatera Utara"),
        IndonesiaProvince("13", "Sumatera Barat"),
        IndonesiaProvince("14", "Riau"),
        IndonesiaProvince("16", "Sumatera Selatan"),
        IndonesiaProvince("31", "DKI Jakarta"),
        IndonesiaProvince("32", "Jawa Barat"),
        IndonesiaProvince("33", "Jawa Tengah"),
        IndonesiaProvince("34", "DI Yogyakarta"),
        IndonesiaProvince("35", "Jawa Timur"),
        IndonesiaProvince("36", "Banten"),
        IndonesiaProvince("51", "Bali"),
        IndonesiaProvince("52", "Nusa Tenggara Barat"),
        IndonesiaProvince("63", "Kalimantan Selatan"),
        IndonesiaProvince("64", "Kalimantan Timur"),
        IndonesiaProvince("71", "Sulawesi Utara"),
        IndonesiaProvince("73", "Sulawesi Selatan"),
        IndonesiaProvince("91", "Papua"),
    ).sortedBy { it.name }

    private fun embeddedFallbackRegencies(provinceId: String, provinceName: String): List<IndonesiaRegency> {
        val all = mapOf(
            "11" to listOf(
                IndonesiaRegency("11.71", "11", "Kota Banda Aceh", provinceName, 5.5483, 95.3238),
            ),
            "12" to listOf(
                IndonesiaRegency("12.71", "12", "Kota Medan", provinceName, 3.5952, 98.6722),
            ),
            "13" to listOf(
                IndonesiaRegency("13.71", "13", "Kota Padang", provinceName, -0.9471, 100.4172),
            ),
            "14" to listOf(
                IndonesiaRegency("14.71", "14", "Kota Pekanbaru", provinceName, 0.5071, 101.4478),
            ),
            "16" to listOf(
                IndonesiaRegency("16.71", "16", "Kota Palembang", provinceName, -2.9761, 104.7754),
            ),
            "31" to listOf(
                IndonesiaRegency("31.71", "31", "Kota Jakarta Pusat", provinceName, -6.1865, 106.8341),
                IndonesiaRegency("31.72", "31", "Kota Jakarta Utara", provinceName, -6.1384, 106.8640),
                IndonesiaRegency("31.73", "31", "Kota Jakarta Barat", provinceName, -6.1674, 106.7637),
                IndonesiaRegency("31.74", "31", "Kota Jakarta Selatan", provinceName, -6.2615, 106.8106),
                IndonesiaRegency("31.75", "31", "Kota Jakarta Timur", provinceName, -6.2250, 106.9004),
            ),
            "32" to listOf(
                IndonesiaRegency("32.73", "32", "Kota Bandung", provinceName, -6.9175, 107.6191),
                IndonesiaRegency("32.71", "32", "Kota Bogor", provinceName, -6.5971, 106.8060),
                IndonesiaRegency("32.75", "32", "Kota Bekasi", provinceName, -6.2383, 106.9756),
                IndonesiaRegency("32.76", "32", "Kota Depok", provinceName, -6.4025, 106.7942),
                IndonesiaRegency("32.74", "32", "Kota Cirebon", provinceName, -6.7320, 108.5523),
            ),
            "33" to listOf(
                IndonesiaRegency("33.74", "33", "Kota Semarang", provinceName, -6.9667, 110.4167),
                IndonesiaRegency("33.72", "33", "Kota Surakarta", provinceName, -7.5755, 110.8243),
                IndonesiaRegency("33.71", "33", "Kota Magelang", provinceName, -7.4797, 110.2177),
            ),
            "34" to listOf(
                IndonesiaRegency("34.71", "34", "Kota Yogyakarta", provinceName, -7.7956, 110.3695),
                IndonesiaRegency("34.04", "34", "Kabupaten Sleman", provinceName, -7.7156, 110.3556),
            ),
            "35" to listOf(
                IndonesiaRegency("35.78", "35", "Kota Surabaya", provinceName, -7.2575, 112.7521),
                IndonesiaRegency("35.73", "35", "Kota Malang", provinceName, -7.9666, 112.6326),
                IndonesiaRegency("35.71", "35", "Kota Kediri", provinceName, -7.8480, 112.0178),
            ),
            "36" to listOf(
                IndonesiaRegency("36.71", "36", "Kota Tangerang", provinceName, -6.1702, 106.6403),
                IndonesiaRegency("36.74", "36", "Kota Tangerang Selatan", provinceName, -6.2886, 106.7179),
                IndonesiaRegency("36.73", "36", "Kota Serang", provinceName, -6.1104, 106.1640),
            ),
            "51" to listOf(
                IndonesiaRegency("51.71", "51", "Kota Denpasar", provinceName, -8.6705, 115.2126),
            ),
            "52" to listOf(
                IndonesiaRegency("52.71", "52", "Kota Mataram", provinceName, -8.5833, 116.1167),
            ),
            "63" to listOf(
                IndonesiaRegency("63.71", "63", "Kota Banjarmasin", provinceName, -3.3194, 114.5908),
            ),
            "64" to listOf(
                IndonesiaRegency("64.71", "64", "Kota Balikpapan", provinceName, -1.2379, 116.8529),
                IndonesiaRegency("64.72", "64", "Kota Samarinda", provinceName, -0.5022, 117.1536),
            ),
            "71" to listOf(
                IndonesiaRegency("71.71", "71", "Kota Manado", provinceName, 1.4748, 124.8421),
            ),
            "73" to listOf(
                IndonesiaRegency("73.71", "73", "Kota Makassar", provinceName, -5.1477, 119.4327),
            ),
            "91" to listOf(
                IndonesiaRegency("91.71", "91", "Kota Jayapura", provinceName, -2.5916, 140.6690),
            ),
        )
        return all[provinceId] ?: listOf(
            IndonesiaRegency(provinceId, provinceId, "Pusat $provinceName", provinceName, -6.2088, 106.8456),
        )
    }
}
