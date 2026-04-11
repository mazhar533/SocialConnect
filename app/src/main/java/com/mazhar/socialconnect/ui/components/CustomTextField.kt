package com.mazhar.socialconnect.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mazhar.socialconnect.ui.theme.BorderColor
import com.mazhar.socialconnect.ui.theme.PrimaryPurple
import com.mazhar.socialconnect.ui.theme.TextFieldBg
import com.mazhar.socialconnect.ui.theme.TextGray

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder, color = TextGray) },
        leadingIcon = icon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = TextGray) }
        },
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPurple,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = TextFieldBg,
            unfocusedContainerColor = TextFieldBg,
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
