package com.billmii.android.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Input Dialog
 * Generic dialog for single line input
 */
@Composable
fun InputDialog(
    title: String,
    placeholder: String = "",
    initialValue: String = "",
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    properties: DialogProperties = DialogProperties()
) {
    var inputValue by remember { mutableStateOf(TextFieldValue(initialValue)) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        isError = false
                        errorMessage = ""
                    },
                    placeholder = { Text(placeholder) },
                    isError = isError,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (isError) {
                        { Text(errorMessage) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (inputValue.text.isBlank()) {
                                isError = true
                                errorMessage = "输入不能为空"
                            } else {
                                onConfirm(inputValue.text)
                            }
                        }
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

/**
 * Multi-line Input Dialog
 */
@Composable
fun MultilineInputDialog(
    title: String,
    placeholder: String = "",
    initialValue: String = "",
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    minLines: Int = 3,
    maxLines: Int = 5,
    properties: DialogProperties = DialogProperties()
) {
    var inputValue by remember { mutableStateOf(TextFieldValue(initialValue)) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        isError = false
                        errorMessage = ""
                    },
                    placeholder = { Text(placeholder) },
                    isError = isError,
                    singleLine = false,
                    minLines = minLines,
                    maxLines = maxLines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    supportingText = if (isError) {
                        { Text(errorMessage) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(dismissText)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (inputValue.text.isBlank()) {
                                isError = true
                                errorMessage = "输入不能为空"
                            } else {
                                onConfirm(inputValue.text)
                            }
                        }
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}