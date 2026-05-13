package com.shkurta.medtrack.di

import android.content.Context
import androidx.room.Room
import com.shkurta.medtrack.data.MedDatabase
import com.shkurta.medtrack.data.dao.MedicationDao
import com.shkurta.medtrack.data.repository.MedicationRepository
import com.shkurta.medtrack.data.repository.MedicationRepositoryImpl
import com.shkurta.medtrack.reminder.ReminderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MedDatabase {
        return Room.databaseBuilder(
            context,
            MedDatabase::class.java,
            MedDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMedicationDao(database: MedDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao,
        reminderManager: ReminderManager
    ): MedicationRepository {
        return MedicationRepositoryImpl(medicationDao, reminderManager)
    }
}
