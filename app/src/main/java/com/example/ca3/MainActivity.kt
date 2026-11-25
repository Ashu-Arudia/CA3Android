
package com.example.ca3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "shopping_items")
data class ShoppingItem(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val quantity: Int
)

@Dao
interface ShoppingDao {

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM shopping_items")
    fun getTotalInternal(): Int

    @Insert
    fun insertInternal(item: ShoppingItem): Long

    @Query("SELECT * FROM shopping_items ORDER BY id ASC")
    fun getItems(): Flow<List<ShoppingItem>>
}


@Database(
    entities = [ShoppingItem::class],
    version = 1,
    exportSchema = false
)
abstract class ShoppingDatabase : RoomDatabase() {

    abstract fun shoppingDao(): ShoppingDao
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ShoppingDatabase.getDatabase(this)
        val dao = db.shoppingDao()

        setContent {
            }
}
