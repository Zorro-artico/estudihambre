package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ContainerEntity
import com.example.data.local.entity.DailyLogEntity
import com.example.data.local.entity.MealEntryEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY logicalDate DESC")
    fun observeAllLogsDesc(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY logicalDate ASC")
    suspend fun getAllLogsAscOnce(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE logicalDate = :logicalDate")
    fun observeLogByDate(logicalDate: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE logicalDate = :logicalDate")
    suspend fun getLogByDateOnce(logicalDate: String): DailyLogEntity?

    @Query("SELECT MAX(dayNumber) FROM daily_logs")
    suspend fun getMaxDayNumber(): Int?

    @Query("SELECT * FROM daily_logs WHERE logicalDate < :logicalDate ORDER BY logicalDate DESC LIMIT 1")
    suspend fun getPreviousLog(logicalDate: String): DailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DailyLogEntity)
}

@Dao
interface MealDao {
    @Query("SELECT * FROM meal_entries ORDER BY createdAtEpochMillis DESC")
    fun observeAllMeals(): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE logicalDate = :logicalDate ORDER BY createdAtEpochMillis ASC")
    fun observeMealsForDate(logicalDate: String): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE isFavorite = 1 ORDER BY createdAtEpochMillis DESC")
    fun observeFavoriteMeals(): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE logicalDate = :logicalDate")
    suspend fun getMealsForDateOnce(logicalDate: String): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries")
    suspend fun getAllMealsOnce(): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getMealByIdOnce(id: Long): MealEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntryEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("DELETE FROM meal_entries WHERE logicalDate = :logicalDate")
    suspend fun deleteMealsForDate(logicalDate: String)
}

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers ORDER BY isFavorite DESC, name ASC")
    fun observeAllContainers(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers WHERE id = :id")
    suspend fun getContainerByIdOnce(id: Long): ContainerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContainer(container: ContainerEntity): Long

    @Query("DELETE FROM containers WHERE id = :id")
    suspend fun deleteContainerById(id: Long)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY logicalDate DESC, recordedAtEpochMillis DESC")
    fun observeAllWeightsDesc(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY logicalDate ASC, recordedAtEpochMillis ASC")
    suspend fun getAllWeightsAscOnce(): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeights(weights: List<WeightEntryEntity>)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteWeightById(id: Long)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMillis ASC")
    fun observeQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMillis ASC")
    suspend fun getQueueOnce(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun updateItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue WHERE mealEntryId = :mealEntryId")
    suspend fun deleteByMealId(mealEntryId: Long)
}
