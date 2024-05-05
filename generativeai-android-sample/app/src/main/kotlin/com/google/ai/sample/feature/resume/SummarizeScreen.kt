/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.sample.feature.resume

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.ai.sample.GenerativeViewModelFactory
import com.google.ai.sample.R
import com.google.ai.sample.ui.theme.Purple80

@Preview
@Composable
internal fun SummarizeRoute(
    summarizeViewModel: SummarizeViewModel = viewModel(factory = GenerativeViewModelFactory)
) {
    val summarizeUiState by summarizeViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    // 设置 NavHost，管理导航内容
    NavHost(navController = navController, startDestination = "resume") {
        composable("profileEditor") { // 首页路由
            ProfileEditor()
        }
        composable("resume") { // 首页路由
            SummarizeScreen(summarizeUiState, navController, onSummarizeClicked = { inputText ->
                summarizeViewModel.summarizeStreaming(inputText)
            })
        }
    }

}

@Composable
fun SummarizeScreen(
    uiState: SummarizeUiState = SummarizeUiState.Loading,
    navController: NavController,
    onSummarizeClicked: (String) -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
//        createTitleRow(title = "个人信息", navController = navController)
//        createTitleRow(title = "教育背景", navController = navController)
//        createTitleRow(title = "在校经历", navController = navController)
//        createTitleRow(title = "荣誉奖项", navController = navController)
//        createTitleRow(title = "项目经历", navController = navController)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            ) {
            Row(
                modifier = Modifier
                    .padding(all = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "点击此处提供简历优化意见",modifier = Modifier.padding(end = 10.dp))
                Image(
                    painter = painterResource(id = R.mipmap.llm),
                    contentDescription = null,

                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            // 个人信息
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {


                Text(
                    fontWeight = FontWeight.Bold,
                    text = " 刘博",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                )
                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent)
                        .clip(CircleShape),
                    onClick = { /* TODO: Implement the click behavior */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .background(Purple80)
                    )
                }
            }
            Text(
                text = " 男 2001-04-03",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        // 假设导航到"profileEditor"
                        navController.navigate("profileEditor")
                    }
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent)
                        .clip(CircleShape),
                    onClick = { /* TODO: Implement the click behavior */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier
                            .size(15.dp)

                    )
                }
                Text(text = "1234567890", modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(bottom = 8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent),
                    onClick = { /* TODO: Implement the click behavior */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        modifier = Modifier
                            .size(15.dp)
                    )
                }
                Text(
                    text = "123456789@qq.com",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(bottom = 8.dp)
                )
            }
            Text(text = "求职意向：Android开发工程师", modifier = Modifier.padding(bottom = 8.dp))
            Text(
                text = "个人优势：\n1.熟悉Java语言，拥有系统设计分析及面向对象分析设计能力,了解常用的设计模式(代理、单例、建造者、观察者、策略等)及反射,自定义注解及泛型；\n" +
                        "2.熟悉Kotlin，有实际的项目开发经验；\n" +
                        "2.了解JVM原理，反射，动态代理;\n" +
                        "3.熟练使用java、jetpack、retrofit、okhttp、rxjava；\n" +
                        "4.有springboot开发和原生微信小程序开发、上线经验；\n" +
                        "4.有良好的编码习惯和学习能力；"
            )
            Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), color = Color.Gray.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 个人信息
                Text(
                    fontWeight = FontWeight.Bold,
                    text = "教育经历",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable {
                            // 假设导航到"profileEditor"
                            navController.navigate("profileEditor")
                        }
                )

                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent)
                        .clip(CircleShape),
                    onClick = { /* TODO: Implement the click behavior */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Purple80)
                    )
                }
            }
            Text(
                text = "福州大学",
                modifier = Modifier.padding(bottom = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,

            )
            Text(
                text = "2020-01 至 2024-01",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        // 假设导航到"profileEditor"
                        navController.navigate("profileEditor")
                    }
            )
            Text(
                text = "计算机类·本科",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        // 假设导航到"profileEditor"
                        navController.navigate("profileEditor")
                    }
            )

            Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), color = Color.Gray.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // 个人信息
                Text(
                    fontWeight = FontWeight.Bold,
                    text = "项目经历",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clickable {
                            // 假设导航到"profileEditor"
                            navController.navigate("profileEditor")
                        }
                )

                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Transparent)
                        .clip(CircleShape),
                    onClick = { /* TODO: Implement the click behavior */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Purple80)
                    )
                }
            }
            Text(
                text = "快手 Android 开发实习生 效率工程部",
                modifier = Modifier.padding(bottom = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,

                )
            Text(
                text = "2023-07 - 至今",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        // 假设导航到"profileEditor"
                        navController.navigate("profileEditor")
                    }
            )
            Text(
                text = "1. 针对群名称搜索查找不便以及查看效率低的问题，对涉及群备注的界面进行了优化改造，包括群聊、消息聚合器、全局pin、收藏等十多个模块。同时，对项目中高亮命中算法进行了优化，并将其下沉为通用工具类。 2. 增加群设置中定时消息的入口以及定时消息列表页、预览页、编辑发送与配置管理能力。使用MVVM架构完成了列表页，使用组件化、数据单向流动、数据转化、收敛列表操作、纯事件观测的toast业务场景、Android原生与flutter交互等技术知识。 3. 对会话分组菜单视图效果进行了优化，原效果为popupWindow缩放动，通过重写View并使用View动画，考虑层级关系、动画时序问题，并封装了弹窗的管理器类。 4. 封装快捷回复横幅通知容器，实现了卡片堆叠渲染等复杂动画，可供其他业务方使用。\n" +
                        "5.完成表情自定义排序功能，扩展公共组件通用能力，解决tablayout和viewpager冲突。 6. 负责OKR子任务，通过逆向等手段调研竞品大文件上传策略，对比竞品压缩、上传性能，提出了优化方案。 6. 针对折叠屏、分屏模式进行了适配，优化了群备注截断策略、完成群设置指定人员发言页面、人员备注以及多个线下、线上Bugfix，实习期间主要负责Kim- Android核心业务IM模块的需求研发和维护 。",
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clickable {
                        // 假设导航到"profileEditor"
                        navController.navigate("profileEditor")
                    }
            )
        }


    }
}

@Composable
fun createTitleRow(
    title: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 个人信息
        Text(
            fontWeight = FontWeight.Bold,
            text = title,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable {
                    // 假设导航到"profileEditor"
                    navController.navigate("profileEditor")
                }
        )

        IconButton(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Transparent)
                .clip(CircleShape),
            onClick = { /* TODO: Implement the click behavior */ }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .background(Purple80)
            )
        }
    }
}

//@Composable
//@Preview(showSystemUi = true)
//fun SummarizeScreenPreview() {
//    GenerativeAISample(darkTheme = true) {
//        SummarizeScreen()
//    }
//}
