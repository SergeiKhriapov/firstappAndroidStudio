import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.auth.Token
import java.io.File

class AuthRepository {

    suspend fun registerUser(name: String, login: String, password: String): Token {
        return AuthApi.service.register(login, password, name)
    }

    suspend fun registerUserWithPhoto(
        name: String,
        login: String,
        password: String,
        avatarFile: File
    ): Token {
        val loginPart: RequestBody = login.toRequestBody("text/plain".toMediaType())
        val passPart: RequestBody = password.toRequestBody("text/plain".toMediaType())
        val namePart: RequestBody = name.toRequestBody("text/plain".toMediaType())
        val filePart = MultipartBody.Part.createFormData(
            "file",
            avatarFile.name,
            avatarFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )

        return AuthApi.service.registerWithPhoto(loginPart, passPart, namePart, filePart)
    }
}
