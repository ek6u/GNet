package com.pira.gnet.di

import android.content.Context
import com.pira.gnet.GNetApplication
import com.pira.gnet.data.ProxyConfig
import com.pira.gnet.utils.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(app: GNetApplication): Context = app

    @Provides
    @Singleton
    fun provideProxyConfig(@ApplicationContext context: Context): MutableStateFlow<ProxyConfig> {
        val preferenceManager = PreferenceManager.getInstance(context)
        val savedConfig = preferenceManager.loadProxySettings()
        return MutableStateFlow(savedConfig)
    }
}