package com.google.ai.sample.feature.resume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun ProfileEditor() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        // 删除和保存按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 标题
            Text(
                text = "教育背景",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { /* 删除逻辑 */ },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("删除")
            }

            Button(
                onClick = { /* 保存逻辑 */ },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存")
            }
        }

        // 学校名称
        Text(
            text = "学校名称",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = "", // TODO: 绑定数据
            onValueChange = {}, // TODO: 更新数据
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // 专业
        Text(
            text = "专业",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = "", // TODO: 绑定数据
            onValueChange = {}, // TODO: 更新数据
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // 学历
        Text(
            text = "学历",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = "", // TODO: 绑定数据
            onValueChange = {}, // TODO: 更新数据
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // 就读时间
        Text(
            text = "就读时间",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp) // 添加一些底部间距以分隔不同的部分
        ) {
            // 入学时间
            OutlinedTextField(
                value = "", // TODO: 绑定数据
                onValueChange = {}, // TODO: 更新数据
                label = { Text("入学时间") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f) // 使两个输入框平分宽度
                    .padding(end = 8.dp) // 添加右侧内边距以分隔两个输入框
            )

            // 毕业时间
            OutlinedTextField(
                value = "", // TODO: 绑定数据
                onValueChange = {}, // TODO: 更新数据
                label = { Text("毕业时间") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f) // 使两个输入框平分宽度
            )
        }

        Text(
            text = "在校经历",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = "", // TODO: 绑定数据
            onValueChange = {}, // TODO: 更新数据
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}