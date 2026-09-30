package id.cukup.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.cukup.data.DbGuard
import id.cukup.data.db.CukupDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): CukupDatabase {
        DbGuard.beforeOpen(context)
        return Room.databaseBuilder(context, CukupDatabase::class.java, DbGuard.DB_NAME)
            .addMigrations(*CukupDatabase.MIGRATIONS)
            // Hanya struktur lama sebelum 0.6 (v1–v3) yang boleh dikosongkan. Mulai v4, migrasi yang
            // hilang membuat aplikasi berhenti alih-alih menghapus data diam-diam; salinan DbGuard tetap ada.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
            .build()
    }
}
