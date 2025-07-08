package ru.netology.nmedia.repository.file

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface FileRepositoryModule {

    @Binds
    @Singleton
    fun bindFileRepository(impl: FileRepositoryImpl): FileRepository
}