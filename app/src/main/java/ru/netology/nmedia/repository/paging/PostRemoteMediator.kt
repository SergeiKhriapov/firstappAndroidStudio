package ru.netology.nmedia.repository.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entities.PostEntity
import ru.netology.nmedia.entities.PostRemoteKeyEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: PostsApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            if (loadType == LoadType.PREPEND) {
                // Отключаем автоматический prepend
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val response = when (loadType) {
                LoadType.REFRESH -> {
                    // ВСЕГДА загружаем последние посты, не зависимо от ключей
                    apiService.getLatest(state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val minKey = postRemoteKeyDao.min() ?: return MediatorResult.Success(true)
                    apiService.getBefore(minKey, state.config.pageSize)
                }

                else -> throw IllegalStateException("Unsupported loadType: $loadType")
            }

            if (!response.isSuccessful) throw HttpException(response)

            val data = response.body().orEmpty()

            if (loadType == LoadType.REFRESH && data.isEmpty()) {
                // Если при REFRESH сервер ничего не вернул, считаем, что данные актуальны, но не error
                return MediatorResult.Success(endOfPaginationReached = false)
            }

            if (loadType == LoadType.APPEND && data.isEmpty()) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        // не очищаем, обновляем или добавляем новые
                        postDao.insert(data.map(PostEntity::fromDto))

                        // Обновляем ключи
                        val maxId = data.maxOfOrNull { it.id } ?: 0L
                        val minId = data.minOfOrNull { it.id } ?: 0L

                        postRemoteKeyDao.insert(
                            listOf(
                                PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.AFTER, maxId),
                                PostRemoteKeyEntity(PostRemoteKeyEntity.KeyType.BEFORE, minId)
                            )
                        )
                    }

                    LoadType.APPEND -> {
                        postDao.insert(data.map(PostEntity::fromDto))

                        // Обновляем ключ BEFORE min ID снизу
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                data.minOf { it.id }
                            )
                        )
                    }

                    else -> {}
                }
            }

            return MediatorResult.Success(endOfPaginationReached = false)

        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        }
    }
}

