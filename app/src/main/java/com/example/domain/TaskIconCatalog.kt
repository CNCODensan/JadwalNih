package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

enum class TaskIconCategory(
    val labelId: String,
    val widgetCategoryKey: String,
) {
    WORK_STUDY("Kerja & Belajar", "work"),
    WORSHIP("Ibadah", "worship"),
    HEALTH("Kesehatan", "health"),
    HOME("Rumah", "home"),
    SOCIAL("Sosial", "social"),
    HOBBY("Hobi & Kreatif", "hobby"),
    TRAVEL("Perjalanan", "travel"),
    FINANCE("Keuangan", "finance"),
}

data class TaskIconSpec(
    val key: String,
    val labelId: String,
    val category: TaskIconCategory,
    val icon: ImageVector,
    val keywords: List<String>,
)

object TaskIconCatalog {

    val allIcons: List<TaskIconSpec> = listOf(
        // 1. Kerja & Belajar (22 icons)
        spec("work", "Kerja", TaskIconCategory.WORK_STUDY, Icons.Rounded.Work, "kerja", "kantor", "office", "proyek"),
        spec("laptop", "Laptop", TaskIconCategory.WORK_STUDY, Icons.Rounded.LaptopMac, "coding", "ngoding", "laptop", "komputer", "program"),
        spec("code", "Ngoding", TaskIconCategory.WORK_STUDY, Icons.Rounded.Code, "code", "dev", "bug", "aplikasi", "software"),
        spec("terminal", "Terminal", TaskIconCategory.WORK_STUDY, Icons.Rounded.Terminal, "server", "cli", "script", "deploy"),
        spec("school", "Kuliah", TaskIconCategory.WORK_STUDY, Icons.Rounded.School, "kuliah", "kampus", "sekolah", "wisuda", "kelas"),
        spec("book", "Baca Buku", TaskIconCategory.WORK_STUDY, Icons.Rounded.MenuBook, "baca", "buku", "materi", "modul", "literasi"),
        spec("auto_stories", "Belajar", TaskIconCategory.WORK_STUDY, Icons.Rounded.AutoStories, "belajar", "ujian", "uts", "uas", "kuis"),
        spec("edit_note", "Catatan", TaskIconCategory.WORK_STUDY, Icons.Rounded.EditNote, "catatan", "nulis", "rangkuman", "pr", "tugas"),
        spec("assignment", "Tugas", TaskIconCategory.WORK_STUDY, Icons.Rounded.Assignment, "laporan", "skripsi", "makalah", "dokumen", "paq"),
        spec("science", "Riset", TaskIconCategory.WORK_STUDY, Icons.Rounded.Science, "fisika", "kimia", "ipa", "eksperimen", "lab", "sains"),
        spec("calculate", "Matematika", TaskIconCategory.WORK_STUDY, Icons.Rounded.Calculate, "matematika", "statistika", "hitung", "aljabar", "rumus"),
        spec("design", "Desain", TaskIconCategory.WORK_STUDY, Icons.Rounded.DesignServices, "desain", "ui", "ux", "figma", "canva"),
        spec("slideshow", "Presentasi", TaskIconCategory.WORK_STUDY, Icons.Rounded.Slideshow, "presentasi", "slide", "demo", "sidang"),
        spec("meeting_room", "Rapat", TaskIconCategory.WORK_STUDY, Icons.Rounded.MeetingRoom, "rapat", "meeting", "diskusi", "evaluasi"),
        spec("video_call", "Meeting Online", TaskIconCategory.WORK_STUDY, Icons.Rounded.VideoCall, "zoom", "meet", "webinar", "online"),
        spec("email", "Email", TaskIconCategory.WORK_STUDY, Icons.Rounded.Email, "email", "surat", "kirim", "inbox"),
        spec("folder", "Arsip", TaskIconCategory.WORK_STUDY, Icons.Rounded.Folder, "arsip", "berkas", "file", "folder"),
        spec("checklist", "Daftar Cek", TaskIconCategory.WORK_STUDY, Icons.Rounded.Checklist, "checklist", "todo", "daftar"),
        spec("schedule", "Agenda", TaskIconCategory.WORK_STUDY, Icons.Rounded.Schedule, "agenda", "jadwal", "reminder"),
        spec("psychology", "Brainstorm", TaskIconCategory.WORK_STUDY, Icons.Rounded.Psychology, "ide", "strategi", "rencana", "riset"),
        spec("translate", "Bahasa", TaskIconCategory.WORK_STUDY, Icons.Rounded.Translate, "inggris", "indonesia", "bahasa", "kursus"),
        spec("biotech", "Biologi", TaskIconCategory.WORK_STUDY, Icons.Rounded.Biotech, "biologi", "medis", "penelitian"),

        // 2. Ibadah & Spiritualitas (14 icons)
        spec("mosque", "Masjid", TaskIconCategory.WORSHIP, Icons.Rounded.Mosque, "sholat", "masjid", "subuh", "dzuhur", "ashar", "maghrib", "isya", "jumat"),
        spec("church", "Gereja", TaskIconCategory.WORSHIP, Icons.Rounded.Church, "gereja", "misa", "ibadah", "kristen", "katolik", "kebaktian"),
        spec("temple_hindu", "Pura", TaskIconCategory.WORSHIP, Icons.Rounded.TempleHindu, "pura", "hindu", "sembahyang", "tri sandhya"),
        spec("temple_buddhist", "Vihara", TaskIconCategory.WORSHIP, Icons.Rounded.TempleBuddhist, "vihara", "buddha", "klenteng", "puja"),
        spec("synagogue", "Rumah Doa", TaskIconCategory.WORSHIP, Icons.Rounded.Synagogue, "doa", "renungan", "ibadah"),
        spec("self_improvement", "Meditasi", TaskIconCategory.WORSHIP, Icons.Rounded.SelfImprovement, "meditasi", "dzikir", "hening", "refleksi", "yoga"),
        spec("nights_stay", "Tahajud", TaskIconCategory.WORSHIP, Icons.Rounded.NightsStay, "tahajud", "malam", "witir", "tarawih"),
        spec("wb_twilight", "Subuh/Senja", TaskIconCategory.WORSHIP, Icons.Rounded.WbTwilight, "subuh", "fajar", "maghrib", "senja"),
        spec("wb_sunny", "Pagi Cerah", TaskIconCategory.WORSHIP, Icons.Rounded.WbSunny, "dhuha", "pagi", "syukur"),
        spec("volunteer_activism", "Sedekah", TaskIconCategory.WORSHIP, Icons.Rounded.VolunteerActivism, "sedekah", "zakat", "donasi", "amal", "berbagi"),
        spec("menu_book_holy", "Kitab Suci", TaskIconCategory.WORSHIP, Icons.Rounded.Book, "quran", "ngaji", "alquran", "alkitab", "tadarus"),
        spec("stars", "Keutamaan", TaskIconCategory.WORSHIP, Icons.Rounded.Stars, "puasa", "ramadan", "lebaran", "natal"),
        spec("diversity_1", "Pengajian", TaskIconCategory.WORSHIP, Icons.Rounded.Diversity1, "kajian", "pengajian", "komunitas"),
        spec("flare", "Cahaya Doa", TaskIconCategory.WORSHIP, Icons.Rounded.Flare, "doa", "syafaat", "syukur"),

        // 3. Kesehatan & Olahraga (18 icons)
        spec("fitness", "Gym", TaskIconCategory.HEALTH, Icons.Rounded.FitnessCenter, "gym", "workout", "fitness", "angkat beban", "olahraga"),
        spec("run", "Lari", TaskIconCategory.HEALTH, Icons.Rounded.DirectionsRun, "lari", "jogging", "marathon", "cardio"),
        spec("walk", "Jalan Kaki", TaskIconCategory.HEALTH, Icons.Rounded.DirectionsWalk, "jalan", "santai", "langkah"),
        spec("bike", "Sepeda", TaskIconCategory.HEALTH, Icons.Rounded.DirectionsBike, "sepeda", "gowes", "cycling"),
        spec("pool", "Renang", TaskIconCategory.HEALTH, Icons.Rounded.Pool, "renang", "kolam", "swim"),
        spec("soccer", "Sepak Bola", TaskIconCategory.HEALTH, Icons.Rounded.SportsSoccer, "bola", "futsal", "sepakbola"),
        spec("basketball", "Basket", TaskIconCategory.HEALTH, Icons.Rounded.SportsBasketball, "basket", "voli"),
        spec("tennis", "Badminton", TaskIconCategory.HEALTH, Icons.Rounded.SportsTennis, "badminton", "bulutangkis", "tenis", "padel"),
        spec("hospital", "Klinik", TaskIconCategory.HEALTH, Icons.Rounded.LocalHospital, "dokter", "klinik", "rumah sakit", "periksa"),
        spec("medication", "Minum Obat", TaskIconCategory.HEALTH, Icons.Rounded.Medication, "obat", "vitamin", "suplemen"),
        spec("favorite", "Jantung", TaskIconCategory.HEALTH, Icons.Rounded.Favorite, "kesehatan", "cek kesehatan", "detak"),
        spec("water_drop", "Minum Air", TaskIconCategory.HEALTH, Icons.Rounded.WaterDrop, "minum", "air", "hidrasi"),
        spec("bedtime", "Tidur", TaskIconCategory.HEALTH, Icons.Rounded.Bedtime, "tidur", "istirahat", "bobo", "siang"),
        spec("spa", "Relaksasi", TaskIconCategory.HEALTH, Icons.Rounded.Spa, "pijat", "skincare", "spa", "perawatan"),
        spec("medical_services", "Medis", TaskIconCategory.HEALTH, Icons.Rounded.MedicalServices, "gigi", "vaksin", "checkup"),
        spec("monitor_heart", "Monitor Sehat", TaskIconCategory.HEALTH, Icons.Rounded.MonitorHeart, "tensi", "terapi", "sehat"),
        spec("hiking", "Hiking", TaskIconCategory.HEALTH, Icons.Rounded.Hiking, "mendaki", "gunung", "trekking"),
        spec("vaccines", "Imunisasi", TaskIconCategory.HEALTH, Icons.Rounded.Vaccines, "suntik", "donor darah"),

        // 4. Rumah, Makan & Kebutuhan (20 icons)
        spec("home", "Di Rumah", TaskIconCategory.HOME, Icons.Rounded.Home, "rumah", "keluarga", "bersih"),
        spec("cleaning", "Beres-beres", TaskIconCategory.HOME, Icons.Rounded.CleaningServices, "bersih", "nyapu", "ngepel", "beres"),
        spec("laundry", "Cuci Baju", TaskIconCategory.HOME, Icons.Rounded.LocalLaundryService, "cuci", "laundry", "setrika", "jemur"),
        spec("restaurant", "Makan", TaskIconCategory.HOME, Icons.Rounded.Restaurant, "makan", "sarapan", "makan siang", "makan malam"),
        spec("coffee", "Ngopi", TaskIconCategory.HOME, Icons.Rounded.LocalCafe, "kopi", "ngopi", "teh", "kafe", "santai"),
        spec("fastfood", "Camilan", TaskIconCategory.HOME, Icons.Rounded.Fastfood, "jajan", "snack", "cemilan"),
        spec("kitchen", "Masak", TaskIconCategory.HOME, Icons.Rounded.Kitchen, "masak", "dapur", "bumbu", "resep"),
        spec("shopping_cart", "Belanja", TaskIconCategory.HOME, Icons.Rounded.ShoppingCart, "belanja", "supermarket", "pasar", "groceries"),
        spec("shopping_bag", "Paket/Beli", TaskIconCategory.HOME, Icons.Rounded.ShoppingBag, "beli", "paket", "shopee", "tokped"),
        spec("pets", "Peliharaan", TaskIconCategory.HOME, Icons.Rounded.Pets, "kucing", "anjing", "hewan", "peliharaan"),
        spec("yard", "Berkebun", TaskIconCategory.HOME, Icons.Rounded.Yard, "siram", "tanaman", "kebun", "taman"),
        spec("build", "Perbaikan", TaskIconCategory.HOME, Icons.Rounded.Build, "servis", "betulin", "bengkel", "tukang"),
        spec("handyman", "Alat Rumah", TaskIconCategory.HOME, Icons.Rounded.Handyman, "listrik", "air", "perkakas"),
        spec("bed", "Kamar", TaskIconCategory.HOME, Icons.Rounded.Bed, "kasur", "sprei", "kamar"),
        spec("chair", "Ruang Tamu", TaskIconCategory.HOME, Icons.Rounded.Chair, "tamu", "sofa", "dekor"),
        spec("bakery", "Roti/Kue", TaskIconCategory.HOME, Icons.Rounded.BakeryDining, "kue", "roti", "bekal"),
        spec("local_grocery", "Sayur Buah", TaskIconCategory.HOME, Icons.Rounded.LocalGroceryStore, "sayur", "buah", "pasar"),
        spec("delete_sweep", "Buang Sampah", TaskIconCategory.HOME, Icons.Rounded.DeleteSweep, "sampah", "daur ulang"),
        spec("lightbulb", "Ide Rumah", TaskIconCategory.HOME, Icons.Rounded.Lightbulb, "lampu", "tagihan"),
        spec("checkroom", "Pakaian", TaskIconCategory.HOME, Icons.Rounded.Checkroom, "baju", "lemari", "packing"),

        // 5. Sosial & Keluarga (16 icons)
        spec("groups", "Organisasi", TaskIconCategory.SOCIAL, Icons.Rounded.Groups, "osis", "panitia", "komunitas", "tim", "organisasi"),
        spec("people", "Ketemu Teman", TaskIconCategory.SOCIAL, Icons.Rounded.People, "nongkrong", "ketemu", "teman", "reuni", "hangout"),
        spec("family", "Keluarga", TaskIconCategory.SOCIAL, Icons.Rounded.FamilyRestroom, "orang tua", "mama", "papa", "ibu", "ayah", "anak", "keluarga"),
        spec("call", "Telepon", TaskIconCategory.SOCIAL, Icons.Rounded.Call, "telepon", "telpon", "telfon", "hubungi"),
        spec("chat", "Chatting", TaskIconCategory.SOCIAL, Icons.Rounded.Chat, "chat", "wa", "whatsapp", "balas pesan"),
        spec("cake", "Ulang Tahun", TaskIconCategory.SOCIAL, Icons.Rounded.Cake, "ulang tahun", "ultah", "pesta", "syukuran"),
        spec("gift", "Kado", TaskIconCategory.SOCIAL, Icons.Rounded.CardGiftcard, "kado", "hadiah", "hantaran", "souvenir"),
        spec("celebration", "Acara", TaskIconCategory.SOCIAL, Icons.Rounded.Celebration, "acara", "perayaan", "nikahan", "kondangan"),
        spec("handshake", "Janji Temu", TaskIconCategory.SOCIAL, Icons.Rounded.Handshake, "klien", "mitra", "kerjasama"),
        spec("child_care", "Anak", TaskIconCategory.SOCIAL, Icons.Rounded.ChildCare, "bayi", "antar anak", "les anak"),
        spec("elderly", "Lansia", TaskIconCategory.SOCIAL, Icons.Rounded.Elderly, "kakek", "nenek", "jenguk"),
        spec("school_bus", "Antar Jemput", TaskIconCategory.SOCIAL, Icons.Rounded.DepartureBoard, "jemput", "antar"),
        spec("forum", "Ngobrol", TaskIconCategory.SOCIAL, Icons.Rounded.Forum, "diskusi", "curhat", "sharing"),
        spec("thumb_up", "Bantu Teman", TaskIconCategory.SOCIAL, Icons.Rounded.ThumbUp, "tolong", "bantu"),
        spec("campaign", "Pengumuman", TaskIconCategory.SOCIAL, Icons.Rounded.Campaign, "sosialisasi", "warga", "rt", "rw"),
        spec("mood", "Me Time", TaskIconCategory.SOCIAL, Icons.Rounded.Mood, "happy", "hiburan", "senang"),

        // 6. Hobi & Kreatif (18 icons)
        spec("music", "Musik", TaskIconCategory.HOBBY, Icons.Rounded.MusicNote, "musik", "gitar", "piano", "nyanyi", "konser"),
        spec("headphones", "Podcast", TaskIconCategory.HOBBY, Icons.Rounded.Headphones, "podcast", "lagu", "audio", "dengerin"),
        spec("movie", "Nonton Film", TaskIconCategory.HOBBY, Icons.Rounded.Movie, "film", "bioskop", "nonton", "drama", "anime"),
        spec("games", "Main Game", TaskIconCategory.HOBBY, Icons.Rounded.SportsEsports, "game", "mabar", "ml", "valorant", "ps"),
        spec("camera", "Fotografi", TaskIconCategory.HOBBY, Icons.Rounded.CameraAlt, "foto", "kamera", "rekap", "vlog", "video"),
        spec("palette", "Melukis", TaskIconCategory.HOBBY, Icons.Rounded.Palette, "gambar", "lukis", "seni", "warna"),
        spec("brush", "Karya", TaskIconCategory.HOBBY, Icons.Rounded.Brush, "kerajinan", "craft", "kaligrafi"),
        spec("mic", "Rekaman", TaskIconCategory.HOBBY, Icons.Rounded.Mic, "rekaman", "mc", "pidato", "voice"),
        spec("piano", "Latihan Alat", TaskIconCategory.HOBBY, Icons.Rounded.Piano, "latihan", "band", "vokal"),
        spec("tv", "Serial TV", TaskIconCategory.HOBBY, Icons.Rounded.Tv, "tv", "youtube", "streaming"),
        spec("auto_awesome", "Kreatif", TaskIconCategory.HOBBY, Icons.Rounded.AutoAwesome, "kreatif", "konten", "tiktok", "reels"),
        spec("edit", "Nulis Cerita", TaskIconCategory.HOBBY, Icons.Rounded.Edit, "jurnal", "diary", "puisi", "novel"),
        spec("library_books", "Koleksi", TaskIconCategory.HOBBY, Icons.Rounded.LibraryBooks, "komik", "manga", "novel"),
        spec("extension", "Puzzle", TaskIconCategory.HOBBY, Icons.Rounded.Extension, "catur", "puzzle", "boardgame"),
        spec("theater", "Teater", TaskIconCategory.HOBBY, Icons.Rounded.TheaterComedy, "teater", "standup", "hiburan"),
        spec("live_tv", "Siaran", TaskIconCategory.HOBBY, Icons.Rounded.LiveTv, "live", "match", "pertandingan"),
        spec("style", "DIY", TaskIconCategory.HOBBY, Icons.Rounded.Style, "diy", "stiker", "koleksi"),
        spec("star", "Favorit", TaskIconCategory.HOBBY, Icons.Rounded.Star, "spesial", "penting", "bintang"),

        // 7. Perjalanan & Lokasi (14 icons)
        spec("car", "Mobil", TaskIconCategory.TRAVEL, Icons.Rounded.DirectionsCar, "mobil", "nyetir", "tol", "bensin"),
        spec("motorcycle", "Motor", TaskIconCategory.TRAVEL, Icons.Rounded.TwoWheeler, "motor", "ojek", "gojek", "grab"),
        spec("bus", "Bus", TaskIconCategory.TRAVEL, Icons.Rounded.DirectionsBus, "bus", "transjakarta", "travel"),
        spec("train", "Kereta", TaskIconCategory.TRAVEL, Icons.Rounded.Train, "kereta", "krl", "mrt", "stasiun", "kai"),
        spec("flight", "Pesawat", TaskIconCategory.TRAVEL, Icons.Rounded.Flight, "pesawat", "bandara", "terbang", "mudik"),
        spec("map", "Rute", TaskIconCategory.TRAVEL, Icons.Rounded.Map, "peta", "rute", "lokasi", "alamat"),
        spec("pin", "Tempat", TaskIconCategory.TRAVEL, Icons.Rounded.LocationOn, "tempat", "kantor", "kampus", "lokasi"),
        spec("luggage", "Liburan", TaskIconCategory.TRAVEL, Icons.Rounded.Luggage, "liburan", "trip", "wisata", "koper"),
        spec("hotel", "Hotel", TaskIconCategory.TRAVEL, Icons.Rounded.Hotel, "hotel", "penginapan", "checkin"),
        spec("beach", "Pantai", TaskIconCategory.TRAVEL, Icons.Rounded.BeachAccess, "pantai", "piknik", "healing"),
        spec("explore", "Jalan-jalan", TaskIconCategory.TRAVEL, Icons.Rounded.Explore, "eksplor", "jalan-jalan", "tamasya"),
        spec("gas", "Isi Bensin", TaskIconCategory.TRAVEL, Icons.Rounded.LocalGasStation, "bensin", "spbu", "pertamina", "isi daya"),
        spec("parking", "Parkir", TaskIconCategory.TRAVEL, Icons.Rounded.LocalParking, "parkir", "cuci mobil"),
        spec("navigation", "Berangkat", TaskIconCategory.TRAVEL, Icons.Rounded.Navigation, "berangkat", "pulang", "otw"),

        // 8. Keuangan & Admin (14 icons)
        spec("wallet", "Dompet", TaskIconCategory.FINANCE, Icons.Rounded.AccountBalanceWallet, "uang", "dompet", "cash", "jajan"),
        spec("bank", "Bank", TaskIconCategory.FINANCE, Icons.Rounded.AccountBalance, "bank", "atm", "transfer", "nabung"),
        spec("payments", "Bayar Tagihan", TaskIconCategory.FINANCE, Icons.Rounded.Payments, "bayar", "tagihan", "listrik", "wifi", "kos", "cicilan"),
        spec("credit_card", "Kartu", TaskIconCategory.FINANCE, Icons.Rounded.CreditCard, "kartu", "kredit", "debit", "qris"),
        spec("receipt", "Struk", TaskIconCategory.FINANCE, Icons.Rounded.ReceiptLong, "struk", "pajak", "bon", "kwitansi"),
        spec("savings", "Tabungan", TaskIconCategory.FINANCE, Icons.Rounded.Savings, "tabungan", "celengan", "investasi", "emas"),
        spec("trending_up", "Saham/Cuan", TaskIconCategory.FINANCE, Icons.Rounded.TrendingUp, "saham", "reksadana", "profit", "cuan"),
        spec("attach_money", "Gaji", TaskIconCategory.FINANCE, Icons.Rounded.AttachMoney, "gaji", "bonus", "honor", "reimburse"),
        spec("store", "Usaha/Toko", TaskIconCategory.FINANCE, Icons.Rounded.Storefront, "jualan", "bisnis", "orderan", "toko"),
        spec("badge", "Identitas/KTP", TaskIconCategory.FINANCE, Icons.Rounded.Badge, "ktp", "sim", "paspor", "dokumen"),
        spec("verified", "Legal/Izin", TaskIconCategory.FINANCE, Icons.Rounded.VerifiedUser, "asuransi", "bpjs", "jaminan"),
        spec("lock", "Keamanan", TaskIconCategory.FINANCE, Icons.Rounded.Lock, "password", "pin", "cadangan", "backup"),
        spec("description", "Kontrak", TaskIconCategory.FINANCE, Icons.Rounded.Description, "kontrak", "form", "pendaftaran"),
        spec("pie_chart", "Anggaran", TaskIconCategory.FINANCE, Icons.Rounded.PieChart, "budget", "anggaran", "keuangan"),
    )

    private val byKeyMap: Map<String, TaskIconSpec> = allIcons.associateBy { it.key }

    val totalCount: Int get() = allIcons.size

    fun byKey(key: String?): TaskIconSpec? {
        val normalized = key?.trim()?.lowercase().orEmpty()
        if (normalized.isEmpty()) return null
        return byKeyMap[normalized]
    }

    fun resolveSpec(iconKey: String?, title: String = ""): TaskIconSpec {
        return byKey(iconKey) ?: byKey(autoDetectIconKey(title)) ?: allIcons.first()
    }

    fun resolveIcon(iconKey: String?, title: String = ""): ImageVector {
        val spec = byKey(iconKey) ?: byKey(autoDetectIconKey(title))
        return spec?.icon ?: Icons.Rounded.EventNote
    }

    fun autoDetectIconKey(title: String): String {
        val lower = title.trim().lowercase()
        if (lower.isEmpty()) return "schedule"

        var bestKey = "schedule"
        var bestScore = 0
        for (item in allIcons) {
            var score = 0
            for (kw in item.keywords) {
                if (lower.contains(kw)) {
                    score += kw.length + 2
                }
            }
            if (lower.contains(item.labelId.lowercase())) {
                score += item.labelId.length + 3
            }
            if (score > bestScore) {
                bestScore = score
                bestKey = item.key
            }
        }
        return bestKey
    }

    fun search(query: String, category: TaskIconCategory? = null): List<TaskIconSpec> {
        val q = query.trim().lowercase()
        return allIcons.filter { spec ->
            (category == null || spec.category == category) &&
                (q.isEmpty() ||
                    spec.labelId.lowercase().contains(q) ||
                    spec.key.lowercase().contains(q) ||
                    spec.keywords.any { it.contains(q) })
        }
    }

    fun widgetDrawableRes(iconKey: String?, title: String = ""): Int {
        val spec = byKey(iconKey) ?: byKey(autoDetectIconKey(title))
        return when (spec?.category) {
            TaskIconCategory.WORK_STUDY -> R.drawable.ic_widget_cat_work
            TaskIconCategory.WORSHIP -> R.drawable.ic_widget_cat_worship
            TaskIconCategory.HEALTH -> R.drawable.ic_widget_cat_health
            TaskIconCategory.HOME -> R.drawable.ic_widget_cat_home
            TaskIconCategory.SOCIAL -> R.drawable.ic_widget_cat_social
            TaskIconCategory.HOBBY -> R.drawable.ic_widget_cat_hobby
            TaskIconCategory.TRAVEL -> R.drawable.ic_widget_cat_travel
            TaskIconCategory.FINANCE -> R.drawable.ic_widget_cat_finance
            null -> R.drawable.ic_widget_cat_default
        }
    }

    private fun spec(
        key: String,
        labelId: String,
        category: TaskIconCategory,
        icon: ImageVector,
        vararg keywords: String,
    ) = TaskIconSpec(
        key = key,
        labelId = labelId,
        category = category,
        icon = icon,
        keywords = keywords.toList(),
    )
}
