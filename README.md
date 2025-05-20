<h1 align="center">Kotlin Error Handling</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-blueviolet?logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/Android-Error%20Handling-green?logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Kotlin-informational?logo=jetpackcompose" />
</p>

<p align="center">
  Clean, testable and extensible error handling structure for Android apps using Kotlin + Flow
</p>

---

[TR 🇹🇷](#tr---kotlin-error-handling-yapısı) | [EN 🇬🇧](#kotlin-error-handling-system)

## Kotlin Error Handling Yapısı

Bu proje, **Android ve Kotlin** tabanlı uygulamalarda kullanılmak üzere tasarlanmış, modüler ve yeniden kullanılabilir bir **API Hata Yönetimi (Error Handling)** altyapısıdır.

## 🚀 Amaç
Karmaşık API yanıtlarını sadeleştirmek, hataları sınıflandırmak ve UI katmanında **`Loading / Success / Error`** durumlarını kolayca yönetmek için `safeApiCall` ve `Resource` yapıları sağlanır.

---

## 📌 Ana Bileşenler

### 🔹 `safeApiCall`
Ağ isteklerini güvenli şekilde saran inline bir fonksiyondur.
- `try-catch` blokları içerir
- Başarılı sonuçları `Resource.Success`, hataları `Resource.Error` olarak döner
- `flowOn(Dispatchers.IO)` ile IO thread’de çalışır

### 🔹 `Resource<T>`
Veri katmanından UI’a üç durum taşıyan bir sealed class:
- `Resource.Success<T>(data)`
- `Resource.Error(apiError)`
- `Resource.Loading`

### 🔹 `ApiError`
Sunucu ve ağ hatalarını sınıflandırır:
- `ClientError`
- `ServerError`
- `NetworkError`
- `UnknownError`
- `HttpError`

### 🔹 `ApiErrorMapper`
API'den dönen hata gövdelerini (`errorBody`) çözümleyerek anlamlı `ApiError` nesnelerine çevirir.
- `DefaultApiErrorMapper` → JSON gövdelerden "message" çeker
- `UserApiErrorMapper` → 403/404 gibi özel durumlar için özelleştirilmiş hata mesajları döner

### 🔹 `collectResource`
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

## 🤝 Katkı Sağlama
PR'lar memnuniyetle karşılanır. Hataları tartışmak ya da yeni hata türleri eklemek için Issue açabilirsiniz.

Her türlü geri bildirim için:
- ✉️ ozgokceoguzhan34@gmail.com
- 🔗 [LinkedIn Profilim](https://www.linkedin.com/in/oğuzhan-özgökce/)

---

**Teşekkürler! Bu yapıyı dilediğiniz Android projesine entegre ederek daha okunabilir, sürdürülebilir ve test edilebilir bir hata yönetimi sağlayabilirsiniz.**

---

<a name="kotlin-error-handling-system"></a>

[EN]
## Kotlin Error Handling System

This project provides a modular and reusable **API Error Handling** infrastructure designed for **Android and Kotlin** applications.

## 📌 Purpose
To simplify complex API responses, categorize errors, and manage **`Loading / Success / Error`** states in the UI layer using `safeApiCall` and `Resource` wrappers.

---

## 🧩 Core Components

### 🔹 `safeApiCall`
An inline function that wraps network calls safely:
- Contains `try-catch` logic
- Emits `Resource.Success` on success and `Resource.Error` on failure
- Runs on the IO dispatcher via `flowOn(Dispatchers.IO)`

### 🔹 `Resource<T>`
A sealed class representing three UI-related states:
- `Resource.Success<T>(data)`
- `Resource.Error(apiError)`
- `Resource.Loading`

### 🔹 `ApiError`
Represents categorized error types:
- `ClientError`
- `ServerError`
- `NetworkError`
- `UnknownError`
- `HttpError`

### 🔹 `ApiErrorMapper`
Parses `errorBody` from the API and converts it to a meaningful `ApiError` instance.
- `DefaultApiErrorMapper` → Extracts `message` field from JSON
- `UserApiErrorMapper` → Custom messages for codes like 403 / 404

### 🔹 `collectResource`
Extension function to handle `Flow<Resource<T>>` emissions more cleanly in the UI:
```kotlin
flow.collectResource(
    onSuccess = { ... },
    onError = { ... },
    onLoading = { ... }
)
```

---

## 🔁 Data Flow Example

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

## 🧪 Testability
- `ApiErrorMapper` can be mocked in unit tests
- `safeApiCall` can simulate network responses
- `Resource` states help in verifying UI behavior

---

## 📂 Folder Structure (Example)

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

## 🤝 Contributing
Pull requests are welcome. Feel free to open an issue to discuss bugs or propose new error types.

---

For feedback or inquiries:
- ✉️ ozgokceoguzhan34@gmail.com
- 🔗 [LinkedIn Profile](https://www.linkedin.com/in/oğuzhan-özgökce/)

---

**Thank you! You can integrate this structure into any Android project to ensure cleaner, maintainable, and test-friendly error handling.**
