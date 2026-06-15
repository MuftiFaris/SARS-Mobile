# SARS-Mobile

Sistem Manajemen Pengajuan Jadwal (SARS) - Schedule Adjustment Request System untuk mobile platform Android. Aplikasi memungkinkan mahasiswa untuk mengajukan perubahan jadwal kuliah dengan proses approval yang terstruktur.

## 📱 Fitur Utama

- **Autentikasi & Manajemen User**
  - Login dengan email/password
  - Support multiple user roles (Mahasiswa, Dosen, Aslab, Admin)
  - Session management dengan Supabase Auth

- **Pengajuan Jadwal (Jadwal Request)**
  - Form wizard 6-step untuk pengajuan perubahan jadwal
  - Support tipe perubahan: Sementara (1x pertemuan) & Permanen (berlaku terus)
  - Validasi jadwal kosong secara realtime
  - Rekomendasi slot jadwal alternatif
  - History pengajuan dengan detail modal

- **Dashboard Role-Based**
  - Student Dashboard: Lihat jadwal & buat pengajuan
  - Lecturer Dashboard: Approve/reject pengajuan mahasiswa
  - Admin Dashboard: Kelola user dan pengajuan sistem
  - Aslab Dashboard: Monitor jadwal lab

- **Notifikasi**
  - Push notification untuk status pengajuan
  - In-app notification system

## 🛠️ Tech Stack

### Frontend
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Compose Navigation
- **State Management**: ViewModel + StateFlow

### Backend & Data
- **Database**: Supabase (PostgreSQL)
- **Real-time**: Supabase Realtime
- **Authentication**: Supabase Auth
- **API**: Supabase Postgrest (ORM-like queries)

### Libraries
- Retrofit (HTTP client backup)
- Coroutines (Async operations)
- OkHttp (Logging interceptor)
- Coil (Image loading)
- Kotlinx Serialization (JSON parsing)

## 📋 Project Structure

```
app/src/main/java/com/informatika/sars/
├── data/
│   ├── model/          # Data classes & DTOs
│   ├── remote/         # API clients (Supabase, Laravel)
│   └── repository/     # Data access layer
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # Screen implementations per role
│   │   ├── auth/
│   │   ├── student/
│   │   ├── lecturer/
│   │   ├── admin/
│   │   └── aslab/
│   └── theme/          # Material 3 theming
├── viewmodel/          # ViewModels untuk state management
├── service/            # Background services (notifications)
└── navigation/         # Navigation graph & routes
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (2024.1 or later)
- JDK 11+
- Android SDK 29+ (minSdk)
- Supabase account & project

### Setup Lokal

1. **Clone Repository**
   ```bash
   git clone https://github.com/MuftiFaris/SARS-Mobile.git
   cd SARS-Mobile
   ```

2. **Configure Supabase**
   - Copy `local.properties.example` → `local.properties`
   - Isi dengan Supabase credentials:
     ```properties
     SUPABASE_URL=your_supabase_url
     SUPABASE_KEY=your_anon_key
     ```

3. **Build & Run**
   ```bash
   ./gradlew build
   ```
   - Open Android Studio → Run app on emulator/device

## 📝 Form Wizard Flow (6 Steps)

### Step 1: Pilih Mata Kuliah
- Filter berdasarkan Kelas → Semester → Mata Kuliah
- Tampil jadwal mata kuliah terpilih

### Step 2: Tipe Perubahan
- **Sementara**: Ubah 1x pertemuan saja
- **Permanen**: Ubah jadwal berlaku terus mulai tanggal efektif

### Step 3: Tanggal/Periode
- Tipe Sementara: Pilih tanggal pertemuan yang mau diubah
- Tipe Permanen: Pilih minggu mulai efektif

### Step 4: Alasan Pengajuan
- Text input minimal 20 karakter
- Deskripsi detail alasan perubahan

### Step 5: Cari Jadwal Pengganti
- Pilih hari pengganti (Senin-Jumat)
- Rekomendasi slot kosong otomatis
- Manual pick sesi waktu + ruangan
- Conflict detection built-in

### Step 6: Review & Konfirmasi
- Review semua data pengajuan
- Submit ke database Supabase
- Redirect ke history setelah sukses

## 🔄 Data Flow

```
StudentRequestScreen (Composable)
    ↓
DashboardViewModel (State Management)
    ↓
RequestRepository (Data Access)
    ↓
SupabaseClient.postgrest (Database)
    ↓
Supabase PostgreSQL
    ↓
validation_requests table
```

## 🗄️ Database Schema

### validation_requests table
```sql
- id (PK)
- request_code (unique)
- requester_id (FK → users)
- schedule_id (FK → schedules)
- request_type (TEMPORARY/PERMANENT)
- target_date (untuk TEMPORARY)
- effective_from_date (untuk PERMANENT)
- proposed_day, proposed_start_time, proposed_end_time
- proposed_room_id
- reason
- status (PENDING/FORWARDED/APPROVED/REJECTED)
- conflict_checked, has_conflict
- created_at, updated_at
```

## 🔐 Security

- ✅ Supabase Row Level Security (RLS) untuk authorization
- ✅ API key diisolasi di BuildConfig (tidak hardcoded)
- ✅ Secrets di .env (excluded dari git)
- ⚠️ JWT token di-refresh otomatis oleh Supabase SDK

## 📱 Supported Android Versions

- Minimum SDK: 29 (Android 10)
- Target SDK: 36 (Android 15)

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/fitur-baru`
2. Commit changes: `git commit -m "feat: deskripsi fitur"`
3. Push branch: `git push origin feature/fitur-baru`
4. Open Pull Request ke main

## 📞 Support & Issues

- Report bugs via GitHub Issues
- Feature requests di Discussions

## 👨‍💻 Developer

**Mufti Faris**
- GitHub: [@MuftiFaris](https://github.com/MuftiFaris)

## 📄 License

MIT License - feel free to use for educational purposes

---

**Last Updated**: Januari 2026
