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
            val response = when (loadType) {
                LoadType.PREPEND -> {
                    // Отключаем автоматический PREPEND
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.REFRESH -> {
                    val maxId = postRemoteKeyDao.max()
                    if (maxId == null) {
                        apiService.getLatest(state.config.pageSize)
                    } else {
                        apiService.getAfter(maxId, state.config.pageSize)
                    }
                }

                LoadType.APPEND -> {
                    val minId = postRemoteKeyDao.min() ?: return MediatorResult.Success(true)
                    apiService.getBefore(minId, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) throw HttpException(response)

            val data = response.body().orEmpty()
            if (data.isEmpty()) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            appDb.withTransaction {

                when (loadType) {
                    LoadType.REFRESH -> {
                        // Не очищаем базу, добавляем новые посты
                        postDao.insert(data.map(PostEntity::fromDto))

                        // Обновить ключ AFTER
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.AFTER,
                                data.first().id
                            )
                        )

                        // Если пустая БД установить
                        if (postDao.isEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    data.last().id
                                )
                            )
                        }

                    }

                    LoadType.APPEND -> {
                        postDao.insert(data.map(PostEntity::fromDto))
                        // обновляем ключ BEFORE
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                data.last().id
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

