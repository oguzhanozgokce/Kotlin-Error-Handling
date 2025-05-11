# Kotlin Error Handling Yapısı

Bu proje, **Android ve Kotlin** tabanlı uygulamalarda kullanılmak üzere tasarlanmış, modüler ve yeniden kullanılabilir bir **API Hata Yönetimi (Error Handling)** altyapısıdır.

## 🚀 Amaç
Karmaşık API yanıtlarını sadeleştirmek, hataları sınıflandırmak ve UI katmanında **`Loading / Success / Error`** durumlarını kolayca yönetmek için `safeApiCall` ve `Resource` yapıları sağlanır.

---

## 📦 Ana Bileşenler

### ✅ `safeApiCall`
Ağ isteklerini güvenli şekilde saran inline bir fonksiyondur.
- `try-catch` blokları içerir
- Başarılı sonuçları `Resource.Success`, hataları `Resource.Error` olarak döner
- `flowOn(Dispatchers.IO)` ile IO thread’de çalışır

### ✅ `Resource<T>`
Veri katmanından UI’a üç durum taşıyan bir sealed class:
- `Resource.Success<T>(data)`
- `Resource.Error(apiError)`
- `Resource.Loading`

### ✅ `ApiError`
Sunucu ve ağ hatalarını sınıflandırır:
- `ClientError`
- `ServerError`
- `NetworkError`
- `UnknownError`
- `HttpError`

### ✅ `ApiErrorMapper`
API'den dönen hata gövdelerini (`errorBody`) çözümleyerek anlamlı `ApiError` nesnelerine çevirir.
- `DefaultApiErrorMapper` → JSON gövdelerden "message" çeker
- `UserApiErrorMapper` → 403/404 gibi özel durumlar için özelleştirilmiş hata mesajları döner

### ✅ `collectResource`
`Flow<Resource<T>>` akışlarını ayrıştırmak için UI tarafında kullanılan kolaylaştırıcı bir fonksiyon.
```kotlin
flow.collectResource(
    onSuccess = { ... },
    onError = { ... },
    onLoading = { ... }
)
```

---

## 🔁 Veri Akışı

```kotlin
// ViewModel
getUsers()
    .onStart { emit(Resource.Loading) }
    .collectResource(
        onSuccess = { users -> ... },
        onError = { error -> ... },
        onLoading = { showSpinner() }
    )

// UseCase
fun invoke(): Flow<Resource<List<User>>> = repository.getUsers()

// Repository
safeApiCall(
    apiCall = { api.getUsers() },
    errorMapper = UserApiErrorMapper()
)
.map { response ->
    when (response) {
        is Resource.Success -> Resource.Success(response.data.map { it.toDomain() })
        else -> response
    }
}
```

---

## 🧪 Test Edilebilirlik
- `ApiErrorMapper` mock edilebilir
- `safeApiCall` içinde ağ çağrıları taklit edilebilir
- `Resource` sayesinde UI testlerinde durumlar açıkça kontrol edilebilir

---

## 📂 Dosya Yapısı (Örnek)

```
errorhandling/
├── ApiError.kt
├── ApiErrorMapper.kt
├── DefaultApiErrorMapper.kt
├── Resource.kt
├── safeApiCall.kt
├── collectResource.kt
└── ErrorResponseDto.kt
```

---

## ✍️ Katkı Sağlama
PR'lar memnuniyetle karşılanır. Hataları tartışmak ya da yeni hata türleri eklemek için Issue açabilirsiniz.


Her türlü geri bildirim için: 
[ozgokceoguzhan34@gmail.com]
[https://www.linkedin.com/in/oğuzhan-özgökce/]

---

**Teşekkürler! Bu yapıyı dilediğiniz Android projesine entegre ederek daha okunabilir, sürdürülebilir ve test edilebilir bir hata yönetimi sağlayabilirsiniz.**
