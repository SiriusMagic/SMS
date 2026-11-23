package com.zirakusa.sms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zirakusa.sms.R
import com.zirakusa.sms.data.Numbers
import com.zirakusa.sms.ui.SmsState

/** «Nuevo mensaje»: the number, then the conversation opens to write. */
@Composable fun NewScreen(state: SmsState) = Sheet(stringResource(R.string.new_message), onClose = state::back) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(state.to, { state.to = it.take(20) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.to)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        if (Numbers.valid(state.to)) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MainButton(stringResource(R.string.next), onClick = state::startNew) }
    }
}
