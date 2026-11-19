
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

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM shopping_items")
    fun getTotalItems(): Int
}


@Database(
    entities = [ShoppingItem::class],
    version = 1,
    exportSchema = false
)
abstract class ShoppingDatabase : RoomDatabase() {

    abstract fun shoppingDao(): ShoppingDao

    companion object {

        @Volatile
        private var INSTANCE: ShoppingDatabase? = null

        fun getDatabase(context: android.content.Context): ShoppingDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingDatabase::class.java,
                    "shopping_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ShoppingDatabase.getDatabase(this)
        val dao = db.shoppingDao()

        setContent {

            var name by remember { mutableStateOf("") }
            var quantity by remember { mutableStateOf("1") }

            var message by remember { mutableStateOf("") }
            var total by remember { mutableIntStateOf(0) }

            var shoppingItems by remember {
                mutableStateOf<List<ShoppingItem>>(emptyList())
            }


            LaunchedEffect(Unit) {

                dao.getItems().collectLatest { items ->

                    shoppingItems = items


                    total = items.sumOf { it.quantity }
                }
            }

            MaterialTheme {

                Scaffold { padding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp)
                    ) {

                        Text(
                            "My Shopping List 🛒",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(Modifier.height(20.dp))


                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text("Total Quantity")

                                Text(
                                    "$total / 3",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))


                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() }) {
                                    quantity = it
                                }
                            },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))


                        Button(
                            onClick = {

                                val qty = quantity.toIntOrNull() ?: 0

                                lifecycleScope.launch(Dispatchers.IO) {
                                    if (name.isBlank() || qty <= 0) {
                                        withContext(Dispatchers.Main) {
                                            message = "Enter valid item name and quantity!"
                                        }
                                        return@launch
                                    }

                                    val currentTotal = dao.getTotalInternal()
                                    if (currentTotal + qty > 3) {
                                        withContext(Dispatchers.Main) {
                                            message = "Error: Maximum quantity limit is 3!"
                                        }
                                        return@launch
                                    }

                                    dao.insertInternal(
                                        ShoppingItem(
                                            name = name.trim(),
                                            quantity = qty
                                        )
                                    )

                                    withContext(Dispatchers.Main) {
                                        message = "Item added successfully!"
                                        name = ""
                                        quantity = "1"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Item")
                        }


                        if (message.isNotEmpty()) {

                            Spacer(Modifier.height(10.dp))

                            Text(
                                message,
                                color = if (
                                    message == "Item added successfully!"
                                ) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "Shopping Items",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {

                            items(shoppingItems) { item ->

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement =
                                            Arrangement.SpaceBetween
                                    ) {

                                        Text(item.name)

                                        Text("Qty: ${item.quantity}")
                                    }
                                }
                            }
                        }

                        Text(
                            "Maximum allowed quantity: 3",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
