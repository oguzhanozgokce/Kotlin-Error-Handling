package com.oguzhanozgokce.errorhandling.data.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import java.io.IOException
import javax.inject.Inject

/**
 * Centralised error‑handling and networking utilities.
 * Created by Oguzhan OZGOKCE on 11.05.2025.
 */

/**
 * Singleton object to provide Gson instance
 */

object GsonProvider {
    val gson: Gson by lazy { GsonBuilder().create() }
}

/**
 * Interface for mapping API errors to a specific error type
 *
 * @param errorBody The error body returned from the API
 * @param errorCode The HTTP status code of the error
 * @return An instance of ApiError representing the mapped error
 */

/** Parses server error bodies into a domain‑specific [ApiError]. */
interface ApiErrorMapper {
    fun mapError(errorBody: String?, errorCode: Int): ApiError
}

/**
 * Sealed class representing different types of API errors
 *
 * @param message The error message
 * @param code The HTTP status code of the error
 */
sealed class ApiError(open val message: String, open val code: Int) {
    companion object {
        const val MSG_NETWORK = "Couldn't reach the server. Please check your connection."
        const val MSG_UNKNOWN = "Unexpected error occurred."
        const val MSG_SERVER = "Server error"
    }
    data class HttpError(override val message: String, override val code: Int) : ApiError(message, code)
    data class NetworkError(override val message: String = MSG_NETWORK, override val code: Int = 0) : ApiError(message, code)
    data class ServerError(override val message: String = MSG_SERVER, override val code: Int) : ApiError(message, code)
    data class ClientError(override val message: String, override val code: Int) : ApiError(message, code)
    data class UnknownError(override val message: String = MSG_UNKNOWN, override val code: Int = 0) : ApiError(message, code)
}

/** Fallback mapper that tries to read "message" field from a JSON body. */

class DefaultApiErrorMapper(
    private val gson: Gson = GsonProvider.gson
) : ApiErrorMapper {
    override fun mapError(errorBody: String?, errorCode: Int): ApiError {
        val parsedMessage = errorBody?.let {
            runCatching { gson.fromJson(it, ErrorResponseDto::class.java).message }.getOrNull()
        }
        val message = parsedMessage ?: "${ApiError.MSG_UNKNOWN} (code: $errorCode)"
        return when (errorCode) {
            in 400..499 -> ApiError.ClientError(message, errorCode)
            in 500..599 -> ApiError.ServerError(message, errorCode)
            else -> ApiError.UnknownError(message, errorCode)
        }
    }
}

/** Wrapper type used throughout data‑to‑UI pipeline. */

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val apiError: ApiError) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

/**
 * Data class representing the error response from the API
 *
 * @param message The error message
 * @param code The HTTP status code of the error
 */
data class ErrorResponseDto(
    val message: String? = null,
    val code: Int? = null,
)

/**
 * Runs [apiCall] in IO, catches common exceptions and emits [Resource].
 *
 * Consumers will typically use [collectResource] on the UI side.
 */

inline fun <T> safeApiCall(
    crossinline apiCall: suspend () -> Response<T>,
    errorMapper: ApiErrorMapper = DefaultApiErrorMapper(),
): Flow<Resource<T>> = flow {
    try {
        val response = apiCall()
        when {
            response.isSuccessful && response.body() != null -> {
                emit(Resource.Success(response.body()!!))
            }
            else -> {
                val errBody = response.errorBody()?.source()?.buffer?.readUtf8()
                emit(Resource.Error(errorMapper.mapError(errBody, response.code())))
            }
        }
    } catch (e: IOException) {
        emit(Resource.Error(ApiError.NetworkError()))
    } catch (e: HttpException) {
        emit(Resource.Error(ApiError.HttpError("${ApiError.MSG_SERVER}: ${e.code()}", e.code())))
    } catch (e: Exception) {
        emit(Resource.Error(ApiError.UnknownError(e.localizedMessage ?: ApiError.MSG_UNKNOWN)))
    }
}.flowOn(Dispatchers.IO)

/**
 * Extension function to collect the Resource flow and handle success, error, and loading states
 *
 * @param onSuccess Lambda function to handle success state
 * @param onError Lambda function to handle error state
 * @param onLoading Lambda function to handle loading state
 * @return A Flow emitting Resource objects representing the result of the API call
 */
inline fun <T> Flow<Resource<T>>.collectResource(
    crossinline onSuccess: (T) -> Unit,
    crossinline onError: (ApiError) -> Unit,
    crossinline onLoading: () -> Unit = {},
): Flow<Resource<T>> = onEach { resource ->
    when (resource) {
        is Resource.Success -> onSuccess(resource.data)
        is Resource.Error -> onError(resource.apiError)
        Resource.Loading -> onLoading()
    }
}


// --------------------------------------------------------------------
// Feature: Users
// --------------------------------------------------------------------


interface ApiService {
    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>
}

/**
 * Data classes representing the User DTO and domain model
 *
 * @param id The user ID
 * @param name The user name
 * @param email The user email
 */
data class UserDto(val id: Int? = null, val name: String ? = null, val email: String? = null)
data class User(val id: Int, val name: String, val email: String)
fun UserDto.toDomain(): User {
    return User(
        id = id ?: 0,
        name = name ?: "",
        email = email ?: ""
    )
}

/**
 * Custom error mapper for user-related API errors
 *
 * @param fallback The fallback error mapper to be used for other errors
 */
class UserApiErrorMapper(
    private val fallback: ApiErrorMapper = DefaultApiErrorMapper()
) : ApiErrorMapper {

    override fun mapError(errorBody: String?, errorCode: Int): ApiError = when (errorCode) {
        404 -> ApiError.ClientError("User not found", 404)
        403 -> ApiError.ClientError("You don't have permission to access this user", 403)
        else -> fallback.mapError(errorBody, errorCode)
    }
}

interface UserRepository {
    fun getUsers(): Flow<Resource<List<User>>>
}


class UserRepositoryImpl(
    private val api: ApiService,
    private val errorMapper: ApiErrorMapper = DefaultApiErrorMapper()
) : UserRepository {

    override fun getUsers(): Flow<Resource<List<User>>> =
        safeApiCall(
            apiCall = { api.getUsers() },
            errorMapper = errorMapper
        ).map { res ->
            when (res) {
                is Resource.Success -> Resource.Success(res.data.map(UserDto::toDomain))
                is Resource.Error -> res
                Resource.Loading -> Resource.Loading
            }
        }
}

// --------------------------------------------------------------------
// Use‑case & ViewModel
// --------------------------------------------------------------------

class GetUsersUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Resource<List<User>>> = repository.getUsers()
}

/**
 * ViewModel for managing user-related UI state
 *
 * @param getUsers The use case for fetching users
 */
data class UserUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String = ""
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val getUsers: GetUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState(isLoading = true))
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        getUsers()
            .onStart { _uiState.update { it.copy(isLoading = true, error = "") } }
            .collectResource(
                onSuccess = { users ->
                    _uiState.update { it.copy(isLoading = false, users = users, error = "") }
                },
                onError = { apiError ->
                    _uiState.update { it.copy(isLoading = false, error = apiError.message) }
                },
                onLoading = {
                    _uiState.update { it.copy(isLoading = true) }
                }
            )
            .launchIn(viewModelScope)
    }
}


