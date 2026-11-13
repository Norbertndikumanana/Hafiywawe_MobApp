package rw.hafiyawe.store.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Toys
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(category: String): ImageVector = when (category) {
    "Phones" -> Icons.Outlined.PhoneAndroid
    "Electronics" -> Icons.Outlined.Devices
    "Clothing" -> Icons.Outlined.Checkroom
    "Furniture" -> Icons.Outlined.Chair
    "Vehicles" -> Icons.Outlined.DirectionsCar
    "Appliances" -> Icons.Outlined.Kitchen
    "Books" -> Icons.Outlined.MenuBook
    "Sports" -> Icons.Outlined.SportsSoccer
    "Toys" -> Icons.Outlined.Toys
    else -> Icons.Outlined.Category
}
