package com.yourname.budgetbee.data.database

import androidx.room.*
import com.yourname.budgetbee.data.models.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalAmountByType(type: String): Flow<Double>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'credit'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'debit'")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsOnce(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE (:type IS NULL OR type = :type) AND (:category IS NULL OR category = :category) ORDER BY date DESC")
    fun getFilteredTransactionsBasic(type: String?, category: String?): Flow<List<Transaction>>

    @Query("SELECT DISTINCT category FROM transactions ORDER BY category")
    suspend fun getAllCategories(): List<String>

    @Query("""
    SELECT * FROM transactions 
    WHERE (:type IS NULL OR type = :type)
      AND (:category IS NULL OR category = :category)
      AND (:startDate IS NULL OR date >= :startDate)
      AND (:endDate IS NULL OR date <= :endDate)
    ORDER BY date DESC
""")
    fun getFilteredTransactions(
        type: String?,
        category: String?,
        startDate: String?,
        endDate: String?
    ): Flow<List<Transaction>>


}
